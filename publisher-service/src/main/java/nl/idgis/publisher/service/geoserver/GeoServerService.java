package nl.idgis.publisher.service.geoserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureTypeLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroupLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureStyle;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.geoserver.rest.Attribute;
import nl.idgis.publisher.service.geoserver.rest.DataStore;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.FeatureType;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.GroupRef;
import nl.idgis.publisher.service.geoserver.rest.Layer;
import nl.idgis.publisher.service.geoserver.rest.LayerGroup;
import nl.idgis.publisher.service.geoserver.rest.LayerRef;
import nl.idgis.publisher.service.geoserver.rest.PublishedRef;
import nl.idgis.publisher.service.geoserver.rest.ServiceSettings;
import nl.idgis.publisher.service.geoserver.rest.ServiceType;
import nl.idgis.publisher.service.geoserver.rest.TiledLayer;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.service.geoserver.rest.WorkspaceSettings;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.XMLUtils;

public class GeoServerService extends UntypedActor {
	
	private static final List<ServiceType> SERVICE_TYPES = Arrays.asList(ServiceType.WMS, ServiceType.WFS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String url, user, password;
	
	private final Map<String, String> databaseConnectionParameters;
	
	private FutureUtils f;
	
	private GeoServerRest rest;

	public GeoServerService(String url, String user, String password, Map<String, String> databaseConnectionParameters) throws Exception {
		this.url = url;
		this.user = user;
		this.password = password;
		this.databaseConnectionParameters = Collections.unmodifiableMap(databaseConnectionParameters);
	}
	
	public static Props props(
		String serviceUrl, String serviceUser, String servicePassword, 
		String databaseUrl, String databaseUser, String databasePassword, 
		
		String schema) {
		
		Pattern urlPattern = Pattern.compile("jdbc:postgresql://(.*):(.*)/(.*)");
		Matcher matcher = urlPattern.matcher(databaseUrl);
		
		if(!matcher.matches()) {
			throw new IllegalArgumentException("incorrect database url");
		}
		
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("host", matcher.group(1));
		connectionParameters.put("port", matcher.group(2));
		connectionParameters.put("database", matcher.group(3));
		connectionParameters.put("user", databaseUser);
		connectionParameters.put("passwd", databasePassword);
		connectionParameters.put("schema", schema);
		
		return Props.create(GeoServerService.class, serviceUrl, serviceUser, servicePassword, connectionParameters);
	}
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext());
		rest = new DefaultGeoServerRest(f, log, url, user, password);		
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		ActorRef initiator = getSender();
		
		if(msg instanceof ServiceIndex) {
			log.debug("service index received");
			
			ServiceIndex serviceIndex = (ServiceIndex)msg;					
			Set<String> serviceNames = serviceIndex.getServiceNames()
				.stream().collect(Collectors.toSet());
			Set<String> styleNames = serviceIndex.getStyleNames()
				.stream().collect(Collectors.toSet());
			
			log.debug("serviceNames: {}", serviceNames);
			log.debug("styleNames: {}", styleNames);
				
			CompletableFuture<List<String>> deletedWorkspaces = 
				rest.getWorkspaces().thenCompose(workspaces ->
					f.sequence(
						workspaces.stream()
							.filter(workspace -> !serviceNames.contains(workspace.getName()))									
							.map(workspace -> rest.deleteWorkspace(workspace)
								.<String>thenApply(v -> workspace.getName()))
							.collect(Collectors.toList())));
			
			CompletableFuture<List<String>> deletedStyles =
				rest.getStyles().thenCompose(styles ->
					f.sequence(
						styles.stream()
							.filter(style -> !styleNames.contains(style.getName()))
							.map(style -> rest.deleteStyle(style)
								.<String>thenApply(v -> style.getName()))
							.collect(Collectors.toList())));
			
			deletedWorkspaces.thenCompose(workspaces ->
				deletedStyles.thenApply(styles -> {
					workspaces.stream().forEach(workspace -> log.debug("workspace deleted: {}", workspace));
					styles.stream().forEach(style -> log.debug("style deleted: {}", style));
					
					return new Vacuumed();
				})).whenComplete((resp, t) -> {
					if(t != null) {
						toSelf(new Failure(t));
					} else {
						toSelf(resp);
					}
				});
			
			getContext().become(vacuuming(initiator));
		} else if(msg instanceof EnsureStyle) {
			EnsureStyle ensureStyle = (EnsureStyle)msg;					
			log.debug("ensure style: {}", ensureStyle.getName());
			
			toSelf(
				rest.getStyle(ensureStyle.getName()).thenCompose(style -> {
					if(style.isPresent()) {
						log.debug("style already present");
						
						try {
							if(XMLUtils.equalsIgnoreWhitespace(
								style.get().getSld(),
								ensureStyle.getSld())) {										
								log.debug("style unchanged");										
								return f.successful(new StyleEnsured());										
							} else {
								log.debug("style changed");
								return rest.putStyle(ensureStyle.getStyle()).thenApply(v -> new StyleEnsured());
							}
						} catch(Exception e) {
							log.error("failed to compare sld documents", e);
							return f.failed(e);
						}
					} else {
						log.debug("style missing");
						
						return rest.postStyle(ensureStyle.getStyle()).thenApply(v -> new StyleEnsured());
					}
				}));
			
			getContext().become(ensuring(initiator));
		} else if(msg instanceof EnsureWorkspace) {
			log.debug("ensure workspace: {}", msg);
			
			EnsureWorkspace ensureWorkspace = (EnsureWorkspace)msg;
			
			String workspaceId = ensureWorkspace.getWorkspaceId();
			
			toSelf(
				rest.getWorkspace(workspaceId).thenCompose(optionalWorkspace -> {
					if(optionalWorkspace.isPresent()) {
						log.debug("existing workspace found: {}", workspaceId);
						
						Workspace workspace = optionalWorkspace.get();
						
						return rest.getDataStore(workspace, "publisher-geometry").thenCompose(optionalDataStore -> {
							if(optionalDataStore.isPresent()) {
								log.debug("existing data store found: publisher-geometry");
								
								DataStore dataStore = optionalDataStore.get();										
								return 
									rest.getFeatureTypes(workspace, dataStore).thenCompose(featureTypes ->
									rest.getLayerGroups(workspace).thenCompose(layerGroups ->
										f.sequence(featureTypes.stream()
											.map(featureType -> rest.getLayer(workspace, featureType))
											.collect(Collectors.toList())).thenCompose(layers -> {
										
									log.debug("feature types and layer groups retrieved");
																				
									return ensureWorkspace(workspace, ensureWorkspace).thenApply(vEnsure ->											
										new EnsuringWorkspace(workspace, dataStore, featureTypes, layerGroups, layers));
								})));
							}
							
							throw new IllegalStateException("publisher-geometry data store is missing");
						});
					}
					
					Workspace workspace = new Workspace(workspaceId);
					return rest.postWorkspace(workspace).thenCompose(vWorkspace -> {								
						log.debug("workspace created: {}", workspaceId);
						DataStore dataStore = new DataStore("publisher-geometry", databaseConnectionParameters);
						return rest.postDataStore(workspace, dataStore).thenCompose(vDataStore -> {									
							log.debug("data store created: publisher-geometry");
							return ensureWorkspace(workspace, ensureWorkspace).thenApply(vEnsure ->
								new EnsuringWorkspace(workspace, dataStore));
						});
					});
				}));
			
			getContext().become(ensuring(initiator));
		} else {
			unhandled(msg);
		}
	}
	
	private static class Vacuumed {}
	
	private Procedure<Object> vacuuming(ActorRef initiator) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Vacuumed) {
					log.debug("vacuum completed");
					vacuumed(JobState.SUCCEEDED);
				} else if(msg instanceof ReceiveTimeout) {
					log.error("timeout while vacuuming");
					vacuumed(JobState.FAILED);
				} else {
					elseProvisioning(msg, initiator);
				}
			}

			private void vacuumed(JobState result) {
				getContext().parent().tell(new UpdateJobState(result), getSelf());
				
				getContext().setReceiveTimeout(Duration.Inf());
				getContext().become(receive());
			}
			
		};
	}
	
	private void elseProvisioning(Object msg, ActorRef initiator) {
		if(msg instanceof Failure) {
			log.error("failure: {}", msg);
			
			ensureFailure(initiator);
		} else {
			log.debug("unhandled: {}", msg);			
			unhandled(msg);
		}
	}

	private void ensureFailure(ActorRef initiator) {
		// TODO: add logging
		getContext().parent().tell(new UpdateJobState(JobState.FAILED), getSelf());
		getContext().become(receive());
	}
	
	private void toSelf(Object msg) {
		ActorRef self = getSelf();
		self.tell(msg, self);
	}
	
	private void toSelf(CompletableFuture<?> future) {
		ActorRef self = getSelf();
		
		future.whenComplete((msg, t) ->  {
			if(t != null) {
				self.tell(new Failure(t), self);
			} else {
				self.tell(msg, self);
			}
		});
	}
	
	private void ensured(ActorRef initiator) {
		log.debug("ensured");
		
		initiator.tell(new Ensured(), getSelf());
	}
	
	private static class StyleEnsured { }
	
	private static class FeatureTypeEnsured {
		
		private final EnsureFeatureTypeLayer ensure;
		
		public FeatureTypeEnsured(EnsureFeatureTypeLayer ensure) {
			this.ensure = ensure;
		}
		
		EnsureFeatureTypeLayer getEnsure() {
			return ensure;
		}
	}
	
	private static class LayerEnsured { }
	
	private static class GroupEnsured { }
	
	private static class WorkspaceEnsured { }
	
	private Procedure<Object> layers(
			ActorRef initiator,
			Workspace workspace, 
			DataStore dataStore,
			Map<String, FeatureType> featureTypes,
			Map<String, LayerGroup> layerGroups,
			Map<String, TiledLayer> tiledLayers,
			Map<String, Layer> layers) {
		
		return layers(null, initiator, workspace, dataStore, featureTypes, layerGroups, tiledLayers, layers);
	}
	
	private Procedure<Object> layers(
			EnsureGroupLayer groupLayer, 
			ActorRef initiator,
			Workspace workspace, 
			DataStore dataStore,
			Map<String, FeatureType> featureTypes,
			Map<String, LayerGroup> layerGroups,
			Map<String, TiledLayer> tiledLayers,
			Map<String, Layer> layers) {
		
		List<PublishedRef> groupLayerContent = new ArrayList<>();
		
		log.debug("-> layers {}", groupLayer == null ? "" : null);
		
		return new Procedure<Object>() {
			
			private boolean unchanged(Layer restLayer, EnsureFeatureTypeLayer ensure) {
				log.debug("checking if layer is changed");
				
				String currentDefaultStyleName = Optional.ofNullable(restLayer.getDefaultStyle())
						.map(styleRef -> styleRef.getStyleName())
						.orElse(null);
				
				if(!Objects.equal(currentDefaultStyleName, ensure.getDefaultStyleName())) {
					log.debug("new default style: {}, was{}", ensure.getDefaultStyleName(), currentDefaultStyleName);
					
					return false;
				}
				
				List<String> currentAdditionalStyleNames = restLayer.getAdditionalStyles().stream()
					.map(styleRef -> styleRef.getStyleName())
					.collect(Collectors.toList());
				
				if(!Objects.equal(currentAdditionalStyleNames, ensure.getAdditionalStyleNames())) {
					log.debug("new additional styles: {}, was {}", ensure.getAdditionalStyleNames(), currentAdditionalStyleNames);
					
					return false;
				}
				
				return true;
			}
			
			private boolean unchanged(FeatureType rest, EnsureFeatureTypeLayer ensure) {
				log.debug("checking if feature type is changed");
				
				if(!Objects.equal(rest.getNativeName(), ensure.getTableName())) {
					log.debug("new table name: {}, was {}", ensure.getTableName(), rest.getNativeName());
					
					return false;
				}
				
				List<String> restColumnNames = rest.getAttributes().stream()
					.map(Attribute::getName)
					.collect(Collectors.toList());
				
				if(!Objects.equal(restColumnNames, ensure.getColumnNames())) {					
					log.debug("new column names: {}, was {}", ensure.getColumnNames(), restColumnNames);
					
					return false;
				}
				
				if(!Objects.equal(rest.getTitle(), ensure.getTitle())) {
					log.debug("new title: {}, was {}", ensure.getTitle(), rest.getTitle());
					
					return false;
				}
				
				if(!Objects.equal(rest.getAbstract(), ensure.getAbstract())) {
					log.debug("new abstract: {}, was {}", ensure.getAbstract(), rest.getAbstract());
					
					return false;
				}
				
				return true;
			}
			
			private boolean unchanged(LayerGroup rest) {
				log.debug("checking if layer group is changed");
				
				if(!Objects.equal(rest.getTitle(), groupLayer.getTitle())) {
					log.debug("new title: {}, was {}", groupLayer.getTitle(), rest.getTitle());
					return false;
				}
				
				if(!Objects.equal(rest.getAbstract(), groupLayer.getAbstract())) {
					log.debug("new abstract: {}, was {}", groupLayer.getAbstract(), rest.getAbstract());
					return false;
				}
				
				if(!rest.getLayers().equals(groupLayerContent)) {
					log.debug("new layer content: {}, was {}", groupLayerContent, rest.getLayers());
					return false;
				}
				
				return true;
			}
			
			void putFeatureType(EnsureFeatureTypeLayer ensureLayer) {
				log.debug("putting feature type");
				
				toSelf(
					rest.putFeatureType(workspace, dataStore, ensureLayer.getFeatureType()).thenApply(v -> {								
						log.debug("feature type updated");									
						return new FeatureTypeEnsured(ensureLayer);
				}));
			}
			
			void postFeatureType(EnsureFeatureTypeLayer ensureLayer) {
				log.debug("posting feature type");
				
				toSelf(
					rest.postFeatureType(workspace, dataStore, ensureLayer.getFeatureType()).thenApply(v -> {								
						log.debug("feature type created");									
						return new FeatureTypeEnsured(ensureLayer);
				}));
			}
			
			void putLayerGroup() {
				log.debug("putting layer group");
				
				toSelf(
					rest.putLayerGroup(workspace, groupLayer.getLayerGroup(groupLayerContent)).thenApply(v -> {
						log.debug("layer group updated");									
						return new GroupEnsured();
				}));
			}
			
			void postLayerGroup() {
				log.debug("posting layer group");
				
				toSelf(
					rest.postLayerGroup(workspace, groupLayer.getLayerGroup(groupLayerContent)).thenApply(v -> {
						log.debug("layer group created");									
						return new GroupEnsured();
				}));
			}
			
			void putLayer(EnsureFeatureTypeLayer ensureLayer) {
				Layer layer = ensureLayer.getLayer();
				
				log.debug("putting layer: {}", layer);
				
				toSelf(
					rest.putLayer(workspace, layer).thenApply(v -> {
						log.debug("layer updated");
						
						return new LayerEnsured();
				}));
				
			}			

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof EnsureLayer) {
					EnsureLayer ensureLayer = (EnsureLayer)msg;
					
					Optional<TiledLayer> tiledLayerOptional = ensureLayer.getTiledLayer();
					if(tiledLayerOptional.isPresent()) {
						log.debug("tiling settings found for layer: {}", ensureLayer.getLayerId());
						
						tiledLayers.put(ensureLayer.getLayerId(), tiledLayerOptional.get());
					}
				}
				
				if(msg instanceof EnsureGroupLayer) {
					EnsureGroupLayer ensureLayer = (EnsureGroupLayer)msg;
					
					ensured(initiator);
					groupLayerContent.add(new GroupRef(ensureLayer.getLayerId()));
					getContext().become(layers(ensureLayer, initiator, 
						workspace, dataStore, featureTypes, layerGroups, tiledLayers, layers), false);
				} else if(msg instanceof EnsureFeatureTypeLayer) {
					EnsureFeatureTypeLayer ensureLayer = (EnsureFeatureTypeLayer)msg;					
					String layerId = ensureLayer.getLayerId();
					
					String groupStyleName = ensureLayer.getGroupStyleName();
					if(groupStyleName == null) {					
						groupLayerContent.add(new LayerRef(layerId));
					} else {
						groupLayerContent.add(new LayerRef(layerId, groupStyleName));
					}
					
					if(featureTypes.containsKey(layerId)) {
						log.debug("existing feature type found: {}", layerId);
						
						FeatureType featureType = featureTypes.get(layerId);
						if(unchanged(featureType, ensureLayer)) {
							log.debug("feature type unchanged");
							toSelf(new FeatureTypeEnsured(ensureLayer));
						} else {
							log.debug("feature type changed");
							putFeatureType(ensureLayer);
						}
						
						featureTypes.remove(layerId);
					} else {
						log.debug("feature type missing: {}", layerId);
						
						postFeatureType(ensureLayer);
					}
				} else if(msg instanceof FeatureTypeEnsured) {
					EnsureFeatureTypeLayer ensureLayer = ((FeatureTypeEnsured)msg).getEnsure();										
					String layerId = ensureLayer.getLayerId();
					
					if(layers.containsKey(layerId)) {
						if(unchanged(layers.get(layerId), ensureLayer)) {
							log.debug("layer unchanged");
							
							toSelf(new LayerEnsured());
						} else {							
							log.debug("layer changed");
							
							putLayer(ensureLayer);
						}
					} else {
						log.debug("layer not exists");
						
						// we don't have to use post because putFeatureType 
						// implicitly created this layer
						putLayer(ensureLayer);
					}
				} else if(msg instanceof LayerEnsured) {
					ensured(initiator);				
				} else if(msg instanceof FinishEnsure) {
					if(groupLayer == null) {
						log.debug("deleting removed items");
						
						List<CompletableFuture<Void>> futures = new ArrayList<>();
						for(FeatureType featureType : featureTypes.values()) {
							log.debug("deleting feature type {}", featureType.getName());
							futures.add(rest.deleteFeatureType(workspace, dataStore, featureType));
						}
						
						for(LayerGroup layerGroup : layerGroups.values()) {
							log.debug("deleting layer group {}", layerGroup.getName());
							futures.add(rest.deleteLayerGroup(workspace, layerGroup));
						}						
									
						/* We can't use the usual 'ensure' strategy for tiled layers because 
						 creating layers (feature types / coverages) implicitly creates
						 a tiled layer that we have to delete when we are done.
						
						 N.B. post and put are used the other way around in GWC*/
						log.debug("configuring tiled layers");
						futures.add(
							rest.getTiledLayerNames(workspace).thenCompose(tiledLayerNames ->
								f.sequence(Arrays.asList(
									f.sequence(
										tiledLayers.entrySet().stream()
											.filter(entry -> !tiledLayerNames.contains(entry.getKey())) // missing tiled layers
											.map(entry -> {																				
												String tiledLayerName = entry.getKey();
												TiledLayer tiledLayer = entry.getValue();
												
												log.debug("putting tiled layer {}", tiledLayerName);
												return rest.putTiledLayer(workspace, tiledLayerName, tiledLayer);
											})
											.collect(Collectors.toList())),
							
									f.sequence(
										tiledLayerNames.stream()
											.map(tiledLayerName -> {	
												if(tiledLayers.containsKey(tiledLayerName)) { // still used tiled layers
													log.debug("posting tiled layer {}", tiledLayerName);
													return rest.postTiledLayer(workspace, tiledLayerName, tiledLayers.get(tiledLayerName));
												} else { // obsolete tiled layers
													log.debug("deleting tiled layer {}", tiledLayerName);
													return rest.deleteTiledLayer(workspace, tiledLayerName);
												}
											})
											.collect(Collectors.toList()))))

									.thenApply(v -> null)));
						
						toSelf(f.sequence(futures).thenApply(v -> new WorkspaceEnsured()));
					} else {
						String groupLayerId = groupLayer.getLayerId();
						
						log.debug("unbecome group {}, groupLayers {}", groupLayerId, groupLayerContent);
						
						if(layerGroups.containsKey(groupLayerId)) {
							log.debug("existing layer group found: " + groupLayerId);
							
							LayerGroup layerGroup = layerGroups.get(groupLayerId);
							if(unchanged(layerGroup)) {
								log.debug("layer group unchanged");
								toSelf(new GroupEnsured());
							} else {
								log.debug("layer group changed");
								putLayerGroup();
							}
							
							layerGroups.remove(groupLayerId);
						} else {
							postLayerGroup();
						}
					}
				} else if(msg instanceof WorkspaceEnsured) {
					log.debug("workspace ensured");
					
					ensured(initiator);
					getContext().parent().tell(new UpdateJobState(JobState.SUCCEEDED), getSelf());
					getContext().unbecome();
				} else if(msg instanceof GroupEnsured) {
					ensured(initiator);
					getContext().unbecome();
				} else {
					elseProvisioning(msg, initiator);
				}
			}						
		};
	}
	
	private static class EnsuringWorkspace {
		
		private final Workspace workspace;
		
		private final DataStore dataStore;
		
		private final Map<String, FeatureType> featureTypes;
		
		private final Map<String, LayerGroup> layerGroups;
		
		private final Map<String, Layer> layers;
		
		EnsuringWorkspace(Workspace workspace, DataStore dataStore) {
			this(workspace, dataStore, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
		}
		
		EnsuringWorkspace(Workspace workspace, DataStore dataStore, List<FeatureType> featureTypes, 
				List<LayerGroup> layerGroups, List<Layer> layers) {
			this.workspace = workspace;
			this.dataStore = dataStore;			
			this.featureTypes = featureTypes.stream()
				.collect(Collectors.toMap(
					featureType -> featureType.getName(),
					Function.identity()));			
			this.layerGroups = layerGroups.stream()
				.collect(Collectors.toMap(
					layerGroup -> layerGroup.getName(),
					Function.identity()));			
			this.layers = layers.stream()
				.collect(Collectors.toMap(
					layer -> layer.getName(),
					Function.identity()));
		}
		
		Workspace getWorkspace() {
			return workspace;
		}
		
		DataStore getDataStore() {
			return dataStore;
		}
		
		Map<String, FeatureType> getFeatureTypes() {
			return featureTypes;
		}
		
		Map<String, LayerGroup> getLayerGroups() {
			return layerGroups;
		}
		
		Map<String, Layer> getLayers() {
			return layers;
		}
	};
	
	private CompletableFuture<Void> ensureWorkspace(Workspace workspace, EnsureWorkspace ensureWorkspace) {
		return ensureServiceSettings(workspace, ensureWorkspace.getServiceSettings())
			.thenCompose(v -> ensureWorkspaceSettings(workspace, ensureWorkspace.getWorkspaceSettings()));					
	}
	
	private CompletableFuture<Void> ensureWorkspaceSettings(Workspace workspace, WorkspaceSettings ensureWorkspaceSettings) {
		return rest.getWorkspaceSettings(workspace).thenCompose(workspaceSettings -> {
			if(workspaceSettings.equals(ensureWorkspaceSettings)) {
				log.debug("workspace settings unchanged");						
				return f.successful(null);
			} else {
				log.debug("workspace settings changed");
				return rest.putWorkspaceSettings(workspace, ensureWorkspaceSettings);
			}
		});
	}
	
	private CompletableFuture<List<Void>> ensureServiceSettings(Workspace workspace, ServiceSettings ensureServiceSettings) {
		return f.sequence(SERVICE_TYPES.stream()
			.map(serviceType -> rest.getServiceSettings(workspace, serviceType))
			.collect(Collectors.toList())).thenCompose(optionalServiceSettings -> {
				return f.sequence(StreamUtils
					.zip(
						SERVICE_TYPES.stream(),
						optionalServiceSettings.stream())
					.map(entry -> {
						ServiceType serviceType = entry.getFirst();
						Optional<ServiceSettings> optionalServiceSettigns = entry.getSecond();								
						if(optionalServiceSettigns.isPresent()) {													
							ServiceSettings serviceSettings = optionalServiceSettigns.get();
							if(serviceSettings.equals(ensureServiceSettings)) {
								log.debug("service settings service type {} unchanged", serviceType);														
								return f.<Void>successful(null);
							} else {
								log.debug("service settings service type {} changed, was: {}, ensure: {}", serviceType, serviceSettings, ensureServiceSettings);														
								return rest.putServiceSettings(workspace, serviceType, ensureServiceSettings);										
							}
						} else {
							log.debug("service settings for service type {} not found", serviceType);
							return rest.putServiceSettings(workspace, serviceType, ensureServiceSettings);									
						}
					})
					.collect(Collectors.toList()));
			});
	}
	
	private Procedure<Object> ensuring(ActorRef initiator) {
		log.debug("-> ensuring");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				 if(msg instanceof EnsuringWorkspace) {
					EnsuringWorkspace workspaceEnsured = (EnsuringWorkspace)msg;
					ensured(initiator);
					
					log.debug("ensuring workspace content");
					Workspace workspace = workspaceEnsured.getWorkspace();
					DataStore dataStore = workspaceEnsured.getDataStore();
					Map<String, FeatureType> featureTypes = workspaceEnsured.getFeatureTypes();
					Map<String, LayerGroup> layerGroups = workspaceEnsured.getLayerGroups();
					Map<String, TiledLayer> tiledLayers = new HashMap<>();
					Map<String, Layer> layers = workspaceEnsured.getLayers();
					getContext().become(layers(initiator, workspace, dataStore, featureTypes, layerGroups, tiledLayers, layers));
				} else if(msg instanceof StyleEnsured) {
					log.debug("style ensured");
					ensured(initiator);
					getContext().become(receive());
				} else {
					elseProvisioning(msg, initiator);
				}
			}
			
		};
	}

	@Override
	public void postStop() {
		try {
			rest.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
