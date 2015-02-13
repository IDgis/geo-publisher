package nl.idgis.publisher.service.geoserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureTypeLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroupLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.geoserver.rest.DataStore;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.FeatureType;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.LayerGroup;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class GeoServerService extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef serviceManager;
	
	private final String serviceLocation, user, password;
	
	private final Map<String, String> connectionParameters;
	
	private GeoServerRest rest;
	
	private FutureUtils f;

	public GeoServerService(ActorRef serviceManager, String serviceLocation, String user, String password, Map<String, String> connectionParameters) throws Exception {		
		this.serviceManager = serviceManager;
		this.serviceLocation = serviceLocation;
		this.user = user;
		this.password = password;
		this.connectionParameters = Collections.unmodifiableMap(connectionParameters);
	}
	
	public static Props props(ActorRef serviceManager, Config geoserverConfig, Config databaseConfig) {
		String serviceLocation = geoserverConfig.getString("url") + "rest/";
		String user = geoserverConfig.getString("user");
		String password = geoserverConfig.getString("password");		
		
		String url = databaseConfig.getString("url");
		
		Pattern urlPattern = Pattern.compile("jdbc:postgresql://(.*):(.*)/(.*)");
		Matcher matcher = urlPattern.matcher(url);
		
		if(!matcher.matches()) {
			throw new IllegalArgumentException("incorrect database url");
		}
		
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("host", matcher.group(1));
		connectionParameters.put("port", matcher.group(2));
		connectionParameters.put("database", matcher.group(3));
		connectionParameters.put("user", databaseConfig.getString("user"));
		connectionParameters.put("passwd", databaseConfig.getString("password"));
		connectionParameters.put("schema", geoserverConfig.getString("schema"));
		
		return Props.create(GeoServerService.class, serviceManager, serviceLocation, user, password, connectionParameters);
	}
	
	@Override
	public void preStart() throws Exception {
		rest = new DefaultGeoServerRest(serviceLocation, user, password);
		f = new FutureUtils(getContext().dispatcher());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ServiceJobInfo) {
			handleServiceJob((ServiceJobInfo)msg);
		} else if(msg instanceof GetActiveJobs) {
			getSender().tell(new ActiveJobs(Collections.<ActiveJob>emptyList()), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private void elseProvisioning(Object msg, ServiceJobInfo serviceJob) {
		if(msg instanceof ServiceJobInfo) {
			// this shouldn't happen
			log.error("receiving service job while provisioning");
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof GetActiveJobs) {
			getSender().tell(new ActiveJobs(Collections.singletonList(new ActiveJob(serviceJob))), getSelf());
		} else if(msg instanceof Failure) {
			// TODO: job failure
		} else {
			unhandled(msg);
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
	
	private void ensured(ActorRef provisioningService) {
		log.debug("ensured");
		
		provisioningService.tell(new Ensured(), getSelf());
	}
	
	private static class LayerEnsured {
		
		public final FeatureType featureType;
		
		LayerEnsured(FeatureType featureType) {
			this.featureType = featureType;
		}
		
		FeatureType getFeatureType() {
			return featureType; 
		}
	}
	
	private static class GroupEnsured {
		
		public final LayerGroup layerGroup;
		
		GroupEnsured(LayerGroup layerGroup) {
			this.layerGroup = layerGroup;
		}
		
		LayerGroup getLayerGroup() {
			return layerGroup;
		}
	}
	
	private Procedure<Object> layers(
			ActorRef initiator, 
			ServiceJobInfo serviceJob, 
			ActorRef provisioningService, 
			Workspace workspace, 
			DataStore dataStore,
			Map<String, FeatureType> featureTypes,
			Map<String, LayerGroup> layerGroups) {
		
		return layers(null, initiator, serviceJob, provisioningService, workspace, dataStore, featureTypes, layerGroups);
	}
	
	private Procedure<Object> layers(
			EnsureGroupLayer groupLayer, 
			ActorRef initiator, 
			ServiceJobInfo serviceJob, 
			ActorRef provisioningService, 
			Workspace workspace, 
			DataStore dataStore,
			Map<String, FeatureType> featureTypes,
			Map<String, LayerGroup> layerGroups) {
		
		List<String> groupLayers = new ArrayList<>();
		
		log.debug("-> group {}", groupLayer);
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof EnsureGroupLayer) {
					ensured(provisioningService);
					getContext().become(layers(((EnsureGroupLayer)msg), initiator, serviceJob, 
						provisioningService, workspace, dataStore, featureTypes, layerGroups), false);
				} else if(msg instanceof EnsureFeatureTypeLayer) {
					EnsureFeatureTypeLayer ensureLayer = (EnsureFeatureTypeLayer)msg;
					
					String layerId = ensureLayer.getLayerId();		
					groupLayers.add(layerId);
					if(featureTypes.containsKey(layerId)) { // TODO: also check feature type content
						log.debug("existing feature type found: " + layerId);
						toSelf(new LayerEnsured(featureTypes.get(layerId)));
						featureTypes.remove(layerId);
					} else {					
						FeatureType featureType = new FeatureType(
							layerId, 
							ensureLayer.getTableName(),
							ensureLayer.getTitle(),
							ensureLayer.getAbstract());
						
						toSelf(
							rest.addFeatureType(workspace, dataStore, featureType).thenApply(v -> {								
								log.debug("feature type created: " + layerId);									
								return new LayerEnsured(featureType);
						}));
					}
				} else if(msg instanceof LayerEnsured) {
					ensured(provisioningService);				
				} else if(msg instanceof FinishEnsure) {
					if(groupLayer == null) {
						log.debug("ack");
						
						ensured(provisioningService);
						initiator.tell(new Ack(), getSelf());
						getContext().unbecome();
					} else {
						String groupLayerId = groupLayer.getLayerId();
						
						log.debug("unbecome group {}, groupLayers {}", groupLayerId, groupLayers);
						
						if(layerGroups.containsKey(groupLayerId)) {  // TODO: also check layer group content
							log.debug("existing layer group found: " + groupLayerId);
							toSelf(new GroupEnsured(layerGroups.get(groupLayerId)));
							layerGroups.remove(groupLayerId);
						} else {
							LayerGroup layerGroup = new LayerGroup(
								groupLayerId,
								groupLayer.getTitle(),
								groupLayer.getAbstract(),
								groupLayers);
							toSelf(
								rest.addLayerGroup(workspace, layerGroup).thenApply(v -> {
									log.debug("layer group created: " + groupLayerId);									
									return new GroupEnsured(layerGroup);
							}));
						}
					}
				} else if(msg instanceof GroupEnsured) {
					ensured(provisioningService);
					getContext().unbecome();
				} else {
					elseProvisioning(msg, serviceJob);
				}
			}				
		};
	}
	
	private static class WorkspaceEnsured {
		
		private final Workspace workspace;
		
		private final DataStore dataStore;
		
		private final Map<String, FeatureType> featureTypes;
		
		private final Map<String, LayerGroup> layerGroups;
		
		WorkspaceEnsured(Workspace workspace, DataStore dataStore) {
			this(workspace, dataStore, new HashMap<>(), new HashMap<>());
		}
		
		WorkspaceEnsured(Workspace workspace, DataStore dataStore, Map<String, FeatureType> featureTypes, Map<String, LayerGroup> layerGroups) {
			this.workspace = workspace;
			this.dataStore = dataStore;
			this.featureTypes = featureTypes;
			this.layerGroups = layerGroups;
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
	};
	
	private Procedure<Object> provisioning(ActorRef initiator, ServiceJobInfo serviceJob, ActorRef provisioningService) {
		log.debug("-> provisioning");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof EnsureWorkspace) {
					String workspaceId = ((EnsureWorkspace)msg).getWorkspaceId();
					
					toSelf(
						rest.getWorkspaces().thenCompose(workspaces -> {
							for(Workspace workspace : workspaces) {
								if(workspace.getName().equals(workspaceId)) {
									log.debug("existing workspace found: {}", workspaceId);
									
									return rest.getDataStores(workspace)
										.thenCompose(f::sequence)
										.thenCompose(dataStores -> {
										for(DataStore dataStore : dataStores) {
											if("publisher-geometry".equals(dataStore.getName())) {
												log.debug("existing data store found: publisher-geometry");
												
												return rest.getFeatureTypes(workspace, dataStore)
													.thenCompose(f::sequence)
													.thenCompose(featureTypes ->
														rest.getLayerGroups(workspace)
														.thenCompose(f::sequence)
														.thenApply(layerGroups -> {
													
													Map<String, FeatureType> featureTypesMap = new HashMap<>();
													Map<String, LayerGroup> layerGroupsMap = new HashMap<>();
													
													for(FeatureType featureType : featureTypes) {
														featureTypesMap.put(featureType.getName(), featureType);
													}
													
													for(LayerGroup layerGroup : layerGroups) {
														layerGroupsMap.put(layerGroup.getName(), layerGroup);
													}
													
													return new WorkspaceEnsured(workspace, dataStore, featureTypesMap, layerGroupsMap);
												}));
											}
										}
										
										throw new IllegalStateException("publisher-geometry data store is missing");
									});
								}
							}
							
							Workspace workspace = new Workspace(workspaceId);
							return rest.addWorkspace(workspace).thenCompose(vWorkspace -> {
								log.debug("workspace created: {}", workspaceId);
								DataStore dataStore = new DataStore("publisher-geometry", connectionParameters);
								return rest.addDataStore(workspace, dataStore).thenApply(vDataStore -> {									
									log.debug("data store created: publisher-geometry");									
									return new WorkspaceEnsured(workspace, dataStore);
								});
							});
						}));					
				} else if(msg instanceof WorkspaceEnsured) {
					WorkspaceEnsured workspaceEnsured = (WorkspaceEnsured)msg;
					
					ensured(provisioningService);
					
					Workspace workspace = workspaceEnsured.getWorkspace();
					DataStore dataStore = workspaceEnsured.getDataStore();
					Map<String, FeatureType> featureTypes = workspaceEnsured.getFeatureTypes();
					Map<String, LayerGroup> layerGroups = workspaceEnsured.getLayerGroups();
					getContext().become(layers(initiator, serviceJob, provisioningService, workspace, dataStore, featureTypes, layerGroups), false);
				} else {
					elseProvisioning(msg, serviceJob);
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
	
	private void handleServiceJob(ServiceJobInfo serviceJob) {
		log.debug("executing service job: " + serviceJob);
		
		ActorRef provisioningService = getContext().actorOf(
				ProvisionService.props(), 
				nameGenerator.getName(ProvisionService.class));
		
		serviceManager.tell(new GetService(serviceJob.getServiceId()), provisioningService);
		
		getContext().become(provisioning(getSender(), serviceJob, provisioningService));
	}
}
