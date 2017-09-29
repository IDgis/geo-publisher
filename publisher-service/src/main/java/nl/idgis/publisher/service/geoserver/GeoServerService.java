package nl.idgis.publisher.service.geoserver;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import nl.idgis.publisher.service.geoserver.messages.EnsureDatasetLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureTypeLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroupLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureCoverageLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureStyle;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.geoserver.rest.CoverageStore;
import nl.idgis.publisher.service.geoserver.rest.DataStore;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.GroupRef;
import nl.idgis.publisher.service.geoserver.rest.Layer;
import nl.idgis.publisher.service.geoserver.rest.LayerRef;
import nl.idgis.publisher.service.geoserver.rest.PublishedRef;
import nl.idgis.publisher.service.geoserver.rest.ServiceSettings;
import nl.idgis.publisher.service.geoserver.rest.ServiceType;
import nl.idgis.publisher.service.geoserver.rest.TiledLayer;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.service.geoserver.rest.WorkspaceSettings;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.XMLUtils;

public class GeoServerService extends UntypedActor {
	
	private static final int SEED_ZOOM_STOP = 9;

	private static final int SEED_ZOOM_START = 0;

	private static final List<ServiceType> SERVICE_TYPES = Arrays.asList(ServiceType.WMS, ServiceType.WFS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String url, user, password;
	
	private final Map<String, String> databaseConnectionParameters;
	
	private final String rasterFolder;
	
	private FutureUtils f;
	
	private GeoServerRest rest;

	public GeoServerService(String url, String user, String password, Map<String, String> databaseConnectionParameters, String rasterFolder) throws Exception {
		this.url = url;
		this.user = user;
		this.password = password;
		this.databaseConnectionParameters = Collections.unmodifiableMap(databaseConnectionParameters);
		this.rasterFolder = rasterFolder;
	}
	
	public static Props props(
		String serviceUrl, String serviceUser, String servicePassword,
		String rasterFolder, String schema) {
		
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("jndiReferenceName", "java:comp/env/jdbc/db");
		connectionParameters.put("fetch size", "1000");
		connectionParameters.put("schema", schema);
		
		return Props.create(GeoServerService.class, serviceUrl, serviceUser, servicePassword, connectionParameters, rasterFolder);
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
				rest.getStyleNames().thenCompose(styles ->
					f.sequence(
						styles.stream()
							.filter(style -> !styleNames.contains(style))
							.map(style -> rest.deleteStyle(style)
								.<String>thenApply(v -> style))
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
			
			getContext().become(vacuuming());
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
			handleEnsureWorkspace((EnsureWorkspace)msg, initiator);
		} else {
			unhandled(msg);
		}
	}

	private void handleEnsureWorkspace(EnsureWorkspace ensureWorkspace, ActorRef initiator) {
		log.debug("ensure workspace: {}", ensureWorkspace);
		
		DataStore dataStore = new DataStore("publisher-geometry", databaseConnectionParameters);
		
		String workspaceId = ensureWorkspace.getWorkspaceId();
		Workspace workspace = new Workspace(workspaceId);
		WorkspaceSettings workspaceSettings = ensureWorkspace.getWorkspaceSettings();
		ServiceSettings serviceSettings = ensureWorkspace.getServiceSettings();
		
		toSelf(
			rest.reset().thenCompose(reset ->
			rest.getWorkspace(workspaceId).thenCompose(existingWorkspace -> {
				if(existingWorkspace.isPresent()) {
					return rest.deleteWorkspace(workspace).thenRun(() -> {
						log.debug("workspace deleted: {}", workspaceId);
					});
				} else {
					log.debug("workspace does not exist: {}", workspaceId);
					return f.successful(null);
				}
			}).thenCompose(delete -> {
				return rest.postWorkspace(workspace).thenCompose(vWorkspace -> {
					log.debug("workspace created: {}", workspaceId);
					return rest.postDataStore(workspace, dataStore).thenCompose(vDataStore -> {
						log.debug("data store created: publisher-geometry");
						return rest.putWorkspaceSettings(workspace, workspaceSettings).thenCompose(vWorkspaceSettings -> {
							log.debug("workspace settings changed: {}", workspaceId);
							return f.supplierSequence(() ->
									SERVICE_TYPES.stream()
										.<Supplier<CompletableFuture<Void>>>map(serviceType ->
											() -> rest.putServiceSettings(workspace, serviceType, serviceSettings))
										.iterator())
								.thenApply(vServiceSettings -> {
									log.debug("service settings changed: {}", workspaceId);
									return new EnsuringWorkspace(workspace, dataStore);
								});
						});
					});
				});
			})));
		
		getContext().become(ensuring(initiator));
	}
	
	private static class Vacuumed {}
	
	private Procedure<Object> vacuuming() {
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
					elseProvisioning(msg);
				}
			}

			private void vacuumed(JobState result) {
				getContext().parent().tell(new UpdateJobState(result), getSelf());
				
				getContext().setReceiveTimeout(Duration.Inf());
				getContext().become(receive());
			}
			
		};
	}
	
	private void elseProvisioning(Object msg) throws Exception {
		if(msg instanceof Failure) {
			handleFailure((Failure)msg);
		} else {
			log.debug("unhandled: {}", msg);			
			unhandled(msg);
		}
	}

	private void handleFailure(Failure failure) throws Exception {
		// TODO: add logging
		log.error("failure: {}", failure);
		
		Throwable cause = failure.getCause();
		if(cause instanceof Exception) {
			throw (Exception)cause;
		} else {
			throw new RuntimeException(cause);
		}
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
	
	private static class DatasetEnsured<T extends EnsureDatasetLayer> {
		
		private final T ensure;
		
		public DatasetEnsured(T ensure) {
			this.ensure = ensure;
		}
		
		T getEnsure() {
			return ensure;
		}
	}
	
	private static class FeatureTypeEnsured extends DatasetEnsured<EnsureFeatureTypeLayer> {
		
		public FeatureTypeEnsured(EnsureFeatureTypeLayer ensure) {
			super(ensure);
		}
	}
	
	private static class CoverageStoreEnsured {
		
		private final EnsureCoverageLayer ensure;
		
		private final CoverageStore coverageStore;
		
		public CoverageStoreEnsured(EnsureCoverageLayer ensure, CoverageStore coverageStore) {
			this.ensure = ensure;
			this.coverageStore = coverageStore;
		}
		
		EnsureCoverageLayer getEnsure() {
			return ensure;
		}
		
		CoverageStore getCoverageStore() {
			return coverageStore;
		}
	}
	
	private static class CoverageEnsured extends DatasetEnsured<EnsureCoverageLayer> {
		
		public CoverageEnsured(EnsureCoverageLayer ensure) {
			super(ensure);
		}
	}
	
	private static class LayerEnsured { }
	
	private static class GroupEnsured { }
	
	private static class WorkspaceEnsured { }
	
	private static class TiledLayerInfo {
		
		final TiledLayer tiledLayer;
		
		final boolean reimported;
		
		TiledLayerInfo(TiledLayer tiledLayer, boolean reimported) {
			this.tiledLayer = tiledLayer;
			this.reimported = reimported;
		}
	}
	
	private Procedure<Object> layers(
			EnsureGroupLayer groupLayer, 
			ActorRef initiator,
			Workspace workspace, 
			DataStore dataStore,
			Map<String, TiledLayerInfo> tiledLayers) {
		
		List<PublishedRef> groupLayerContent = new ArrayList<>();
		
		log.debug("-> layers {}", groupLayer == null ? "" : null);
		
		return new Procedure<Object>() {
			
			URL getRasterUrl(String fileName) throws MalformedURLException {
				log.debug("creating raster url for fileName: {}, rasterFolder: {}", fileName, rasterFolder);
				
				return Paths.get(rasterFolder, fileName).toUri().toURL();
			}
			
			String getCoverageStoreName(EnsureCoverageLayer ensureLayer) {
				return "publisher-raster-" + ensureLayer.getNativeName();
			}
			
			void postCoverageStore(EnsureCoverageLayer ensureLayer) throws MalformedURLException {
				String fileName = ensureLayer.getFileName();
				
				String name = getCoverageStoreName(ensureLayer);
				URL url = getRasterUrl(fileName);
				
				CoverageStore coverageStore = new CoverageStore(name, url);
				
				toSelf(
					rest.postCoverageStore(workspace, coverageStore).thenApply(v -> {
						log.debug("coverage store created");
						
						return new CoverageStoreEnsured(ensureLayer, coverageStore);
				}));
			}
			
			void postCoverage(CoverageStore coverageStore, EnsureCoverageLayer ensureLayer) {
				toSelf(
					rest.postCoverage(workspace, coverageStore, ensureLayer.getCoverage()).thenApply(v -> {
						log.debug("coverage created");
						
						return new CoverageEnsured(ensureLayer);
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
			
			void postLayerGroup() {
				log.debug("posting layer group");
				
				toSelf(
					rest.postLayerGroup(workspace, groupLayer.getLayerGroup(groupLayerContent)).thenApply(v -> {
						log.debug("layer group created");
						return new GroupEnsured();
				}));
			}
			
			void putLayer(EnsureDatasetLayer ensureLayer) {
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
						
						tiledLayers.put(ensureLayer.getLayerId(), new TiledLayerInfo(tiledLayerOptional.get(), ensureLayer.isReimported()));
					} else {
						log.debug("no tiling settings found for layer: {}", ensureLayer.getLayerId());
					}
				}
				
				if(msg instanceof EnsureGroupLayer) {
					EnsureGroupLayer ensureLayer = (EnsureGroupLayer)msg;
					
					ensured(initiator);
					groupLayerContent.add(new GroupRef(ensureLayer.getLayerId()));
					getContext().become(layers(ensureLayer, initiator, 
						workspace, dataStore, tiledLayers), false);
				} else if(msg instanceof EnsureFeatureTypeLayer) {
					EnsureFeatureTypeLayer ensureLayer = (EnsureFeatureTypeLayer)msg;
					String layerId = ensureLayer.getLayerId();
					
					String groupStyleName = ensureLayer.getGroupStyleName();
					if(groupStyleName == null) {					
						groupLayerContent.add(new LayerRef(layerId));
					} else {
						groupLayerContent.add(new LayerRef(layerId, groupStyleName));
					}
					
					log.debug("feature type missing: {}", layerId);
						
					postFeatureType(ensureLayer);
				} else if(msg instanceof EnsureCoverageLayer) {
					EnsureCoverageLayer ensureLayer = (EnsureCoverageLayer)msg;
					String layerId = ensureLayer.getLayerId();
					String coveragStoreName = getCoverageStoreName(ensureLayer);
					
					String groupStyleName = ensureLayer.getGroupStyleName();
					if(groupStyleName == null) {
						groupLayerContent.add(new LayerRef(layerId));
					} else {
						groupLayerContent.add(new LayerRef(layerId, groupStyleName));
					}
					
					log.debug("coverage store missing: {}", coveragStoreName);
						
					postCoverageStore(ensureLayer);
				} else if(msg instanceof CoverageStoreEnsured) {
					CoverageStoreEnsured ensured = (CoverageStoreEnsured)msg;
					
					CoverageStore coverageStore = ensured.getCoverageStore();
					EnsureCoverageLayer ensureLayer = ensured.getEnsure();
					
					postCoverage(coverageStore, ensureLayer);
				} else if(msg instanceof DatasetEnsured) {
					EnsureDatasetLayer ensureLayer = ((DatasetEnsured<?>)msg).getEnsure();
						
					// we don't have to use post because putFeatureType 
					// implicitly created this layer
					putLayer(ensureLayer);
				} else if(msg instanceof LayerEnsured) {
					ensured(initiator);
				} else if(msg instanceof FinishEnsure) {
					if(groupLayer == null) {
						toSelf(
							rest.getTiledLayerNames(workspace).thenCompose(tiledLayerNames -> {
								List<Supplier<CompletableFuture<Void>>> futures = new ArrayList<>();
								
								/* We can't use the usual strategy for tiled layers because 
								 creating layers (feature types / coverages) implicitly creates
								 a tiled layer that we have to delete when we don't need it.
								
								N.B. post and put are used the other way around in GWC */
								log.debug("configuring tiled layers");
								for(Map.Entry<String, TiledLayerInfo> entry : tiledLayers.entrySet()) {
									if(!tiledLayerNames.contains(entry.getKey())) {
										String tiledLayerName = entry.getKey();
										TiledLayer tiledLayer = entry.getValue().tiledLayer;
										
										log.debug("putting tiled layer {}", tiledLayerName);
										futures.add(() -> rest.putTiledLayer(workspace, tiledLayerName, tiledLayer));
										
										log.debug("seeding tiled layer {}", tiledLayerName);
										futures.add(() -> rest.seedTiledLayer(workspace, tiledLayerName, tiledLayer, SEED_ZOOM_START, SEED_ZOOM_STOP));
									}
								}
								
								for(String tiledLayerName : tiledLayerNames) {
									if(tiledLayers.containsKey(tiledLayerName)) { // still used tiled layers
										log.debug("posting tiled layer {}", tiledLayerName);
										
										TiledLayerInfo tiledLayerInfo = tiledLayers.get(tiledLayerName);
										
										futures.add(() -> rest.postTiledLayer(workspace, tiledLayerName, tiledLayerInfo.tiledLayer));
										
										if(tiledLayerInfo.reimported) {
											log.debug("reseeding tiled layer {}", tiledLayerName);
											futures.add(() -> rest.reseedTiledLayer(workspace, tiledLayerName, tiledLayerInfo.tiledLayer, SEED_ZOOM_START, SEED_ZOOM_STOP));
										} else {
											log.debug("reseeding tiled layer ({}) not necessary", tiledLayerName);
										}
									} else { // obsolete tiled layers
										log.debug("deleting tiled layer {}", tiledLayerName);
										futures.add(() -> rest.deleteTiledLayer(workspace, tiledLayerName));
									}
								}
								
								return f.supplierSequence(futures).thenApply(v -> new WorkspaceEnsured());
							}));
					} else {
						String groupLayerId = groupLayer.getLayerId();
						
						log.debug("unbecome group {}, groupLayers {}", groupLayerId, groupLayerContent);
						
						postLayerGroup();
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
					elseProvisioning(msg);
				}
			}									
		};
	}
	
	private static class EnsuringWorkspace {
		
		private final Workspace workspace;
		
		private final DataStore dataStore;
		
		
		EnsuringWorkspace(Workspace workspace, DataStore dataStore) {
			this.workspace = workspace;
			this.dataStore = dataStore;
		}
		
		Workspace getWorkspace() {
			return workspace;
		}
		
		DataStore getDataStore() {
			return dataStore;
		}
		
	};
	
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
					getContext().become(layers(null, initiator, workspace, dataStore, new HashMap<>()));
				} else if(msg instanceof StyleEnsured) {
					log.debug("style ensured");
					ensured(initiator);
					getContext().become(receive());
				} else {
					elseProvisioning(msg);
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
