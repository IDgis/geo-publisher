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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.metadata.messages.DatasetInfo;
import nl.idgis.publisher.metadata.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.messages.GetServiceMetadata;
import nl.idgis.publisher.metadata.messages.MetadataInfo;
import nl.idgis.publisher.metadata.messages.ServiceInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class MetadataInfoProcessor extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef initiator, metadataSource, metadataTarget;
		
	public MetadataInfoProcessor(ActorRef initiator, ActorRef metadataSource, ActorRef metadataTarget) {
		this.initiator = initiator;
		this.metadataSource = metadataSource;
		this.metadataTarget = metadataTarget;
	}
	
	public static Props props(ActorRef initiator, ActorRef metadataSource, ActorRef metadataTarget) {
		return Props.create(
			MetadataInfoProcessor.class, 
			Objects.requireNonNull(initiator, "initiator must not be null"), 
			Objects.requireNonNull(metadataSource, "metadataSource must not be null"),
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"));
	}
	
	@Override
	public final void preStart() {		
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

					metadataSource.tell(
						new GetServiceMetadata(serviceId), 
						getContext().actorOf(
							ServiceMetadataGenerator.props(metadataTarget, serviceInfo),
							nameGenerator.getName(ServiceMetadataGenerator.class)));
				} else {
					log.debug("all services processed");
					
					terminate();
				}
			}
			
		};
	}			
	
	private Procedure<Object> traversingDatasets(
		MetadataInfo metadataInfo,
		Iterator<DatasetInfo> datasetItr) {
		
		return new Procedure<Object>() {

			DatasetInfo currentDataset;

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("message received while traversing datasets: {}", msg);
				
				if(datasetItr.hasNext()) {
					currentDataset = datasetItr.next();
					
					metadataSource.tell(
						new GetDatasetMetadata(currentDataset.getDataSourceId(), currentDataset.getExternalDatasetId()),
						getContext().actorOf(
							DatasetMetadataGenerator.props(metadataTarget, currentDataset),
							nameGenerator.getName(DatasetMetadataGenerator.class)));
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
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof MetadataInfo) {
			log.debug("metadata info received");
			
			MetadataInfo metadataInfo = (MetadataInfo)msg;
			
			getContext().become(
					traversingDatasets(
						metadataInfo,
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
