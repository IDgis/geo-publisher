package nl.idgis.publisher.metadata;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.metadata.MetadataGenerator.layerGenericLayer;
import static nl.idgis.publisher.metadata.MetadataGenerator.serviceGenericLayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.messages.DatasetInfo;
import nl.idgis.publisher.metadata.messages.MetadataInfo;
import nl.idgis.publisher.metadata.messages.ServiceInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.StreamUtils;

public class MetadataInfoProcessor extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef initiator, harvester;
	
	private final MetadataStore serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget;
	
	private final Config constants;
	
	private FutureUtils f;
	
	public MetadataInfoProcessor(ActorRef initiator, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		this.initiator = initiator;
		this.harvester = harvester;
		this.serviceMetadataSource = serviceMetadataSource;
		this.datasetMetadataTarget = datasetMetadataTarget;
		this.serviceMetadataTarget = serviceMetadataTarget;
		this.constants = constants;	
	}
	
	public static Props props(ActorRef initiator, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		return Props.create(MetadataInfoProcessor.class, initiator, harvester, serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget, constants);
	}
	
	@Override
	public final void preStart() {
		f = new FutureUtils(getContext());
		
		getContext().setReceiveTimeout(Duration.create(10, TimeUnit.SECONDS));
	}
	
	private static Map<String, Set<String>> servicesToMap(Service service) {
		Map<String, Set<String>> layerInfo = new HashMap<>();
		traverseLayers(layerInfo, service.getLayers());
		
		return layerInfo;
	}
	
	private Map<String, Map<String, Set<String>>> servicesToMap(List<Service> services) {
		return services.stream()
			.collect(Collectors.toMap(
				service -> service.getId(),
				MetadataInfoProcessor::servicesToMap));
	}
	
	private static void traverseLayers(Map<String, Set<String>> layerInfo, List<LayerRef<? extends Layer>> layerRefs) {
		for(LayerRef<? extends Layer> layerRef : layerRefs) {
			if(layerRef.isGroupRef()) {
				GroupLayer groupLayer = layerRef.asGroupRef().getLayer();
				traverseLayers(layerInfo, groupLayer.getLayers());
			} else {
				Layer layer = layerRef.getLayer();

				Set<String> layerNames;
				String layerId = layer.getId();
				if(layerInfo.containsKey(layerId)) {
					layerNames = layerInfo.get(layerId);
				} else {
					layerNames = new HashSet<>();
					layerInfo.put(layerId, layerNames);
				}
				
				layerNames.add(layer.getName());
			}
		}
	}
	
	private Procedure<Object> traversingServices(
		MetadataInfo metadataInfo,
		Iterator<ServiceInfo> serviceItr) {
		
		return new Procedure<Object>() {
			
			ServiceInfo serviceInfo;

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("message received while traversing services: {}", msg);
				
				if(serviceItr.hasNext()) {
					serviceInfo = serviceItr.next();
					
					String serviceId = serviceInfo.getId();
					log.debug("requesting metadata for service: {}", serviceId);
					
					ActorRef generator = getContext().actorOf(ServiceMetadataGenerator.props(serviceInfo));
					
					serviceMetadataSource.get(serviceId).whenComplete((metadataDocument, throwable) -> {
						if(throwable == null) {
							generator.tell(metadataDocument, getSelf());
						} else {
							generator.tell(new Failure(throwable), getSelf());
						}
					});
				} else {
					log.debug("all services processed");
					
					terminate();
				}
			}
			
		};
	}			
	
	private Procedure<Object> traversingDatasets(
		MetadataInfo metadataInfo,
		Map<String, ActorRef> dataSources, 
		Iterator<DatasetInfo> datasetItr) {
		
		return new Procedure<Object>() {

			DatasetInfo currentDataset;

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("message received while traversing datasets: {}", msg);
				
				if(datasetItr.hasNext()) {
					currentDataset = datasetItr.next();
					
					String datasetId = currentDataset.getId();
					String dataSourceId = currentDataset.getDataSourceId();
					if(dataSources.containsKey(dataSourceId)) {						
						dataSources.get(dataSourceId).tell(
							new GetDatasetMetadata(currentDataset.getExternalDatasetId()), 
							getContext().actorOf(DatasetMetadataGenerator.props(currentDataset)));
					} else {
						log.warning("cannot process dataset {} because dataSource {} is not available", 
							datasetId, dataSourceId);
					}
				} else {
					log.debug("all datasets processed");
					
					log.debug("traversing services");
					
					getContext().become(
						traversingServices(
							metadataInfo, 
							metadataInfo.getJoinTuples().stream()
								.collect(Collectors.groupingBy(
									tuple -> tuple.get(serviceGenericLayer.identification),
									Collectors.mapping(
										tuple -> tuple.get(dataset.identification),
										Collectors.toSet()))).entrySet().stream()
											.map(entry -> new ServiceInfo(entry.getKey(), entry.getValue()))
											.iterator()));
					
					getSelf().tell(new NextItem(), getSelf());
				}
			}
			
		};
	}
	
	static class DataSourceReceived {
		
		private final String dataSourceId;
		
		private final ActorRef actorRef;
		
		public DataSourceReceived(String dataSourceId, ActorRef actorRef) {
			this.dataSourceId = dataSourceId;
			this.actorRef = actorRef;
		}

		public String getDataSourceId() {
			return dataSourceId;
		}

		public ActorRef getActorRef() {
			return actorRef;
		}
	}
	
	static class DataSourceFailure {
		
		private final String dataSourceId;
				
		public DataSourceFailure(String dataSourceId) {
			this.dataSourceId = dataSourceId;			
		}

		public String getDataSourceId() {
			return dataSourceId;
		}
	}
	
	private Procedure<Object> receivingDataSources(Set<String> dataSourceIds, MetadataInfo metadataInfo) {
		
		return new Procedure<Object>() {
			
			Map<String, ActorRef> dataSources = new HashMap<>();
			
			private void traverseDatasets() {
				log.debug("traversing datasets");
				
				getContext().become(
						traversingDatasets(
							metadataInfo,
							dataSources,
							metadataInfo.getJoinTuples().stream()
								.map(StreamUtils.wrap(tuple -> tuple.get(dataset.identification)))
								.distinct()
								.map(StreamUtils.Wrapper::unwrap)
								.map(tuple -> new DatasetInfo(
									tuple.get(dataset.identification), 
									tuple.get(dataSource.identification),
									tuple.get(sourceDataset.externalIdentification)))
								.iterator()));
					
				getSelf().tell(new NextItem(), getSelf());
			}

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("message received while receiving dataSources: {}", msg);
				
				if(msg instanceof DataSourceReceived) {
					DataSourceReceived dataSourceReceived = (DataSourceReceived)msg;					
					String dataSourceId = dataSourceReceived.getDataSourceId();
					
					log.debug("dataSource received: {}", dataSourceId);
					dataSources.put(dataSourceId, dataSourceReceived.getActorRef());
					dataSourceIds.remove(dataSourceId);
				} else if(msg instanceof DataSourceFailure) {
					String dataSourceId = ((DataSourceFailure) msg).getDataSourceId(); 
					
					log.debug("dataSource failure: {}", dataSourceId);
					dataSourceIds.remove(dataSourceId);
				} else if(msg instanceof ReceiveTimeout) {
					log.error("timeout while receiving dataSources");
					
					if(dataSources.isEmpty()) {
						terminate();
					} else {
						traverseDatasets();
					}
				}
				
				if(dataSourceIds.isEmpty()) {
					log.debug("all dataSources received");
					
					traverseDatasets();
				}
			}
			
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof MetadataInfo) {
			log.debug("metadata info received");
			
			MetadataInfo metadataInfo = (MetadataInfo)msg;			
			List<Tuple> joinTuples = metadataInfo.getJoinTuples();
			
			Set<String> dataSourceIds =
				joinTuples.stream()
					.map(tuple -> tuple.get(dataSource.identification))
					.distinct()
					.map(dataSourceId -> {
						f.ask(harvester, new GetDataSource(dataSourceId))
							.thenAccept(harvesterResponse -> {
								if(harvesterResponse instanceof ActorRef) {
									getSelf().tell(new DataSourceReceived(dataSourceId, (ActorRef)harvesterResponse), getSelf());
								} else {
									getSelf().tell(new DataSourceFailure(dataSourceId), getSelf());
								}
							});
						
						return dataSourceId;
					})
					.collect(Collectors.toSet());
		
			
			getContext().become(receivingDataSources(dataSourceIds, metadataInfo));
		} else {
			unhandled(msg);
		}
	}

	private void terminate() {
		log.debug("terminating");
		
		initiator.tell(new Ack(), getContext().parent());
		getContext().stop(getSelf());
	}	

}
