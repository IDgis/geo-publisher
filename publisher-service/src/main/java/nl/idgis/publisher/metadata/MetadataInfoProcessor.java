package nl.idgis.publisher.metadata;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.metadata.messages.BeginMetadataUpdate;
import nl.idgis.publisher.metadata.messages.CommitMetadata;
import nl.idgis.publisher.metadata.messages.DatasetInfo;
import nl.idgis.publisher.metadata.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.messages.GetServiceMetadata;
import nl.idgis.publisher.metadata.messages.MetadataInfo;
import nl.idgis.publisher.metadata.messages.ServiceInfo;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class MetadataInfoProcessor extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef initiator, metadataSource, metadataTarget;
	
	private final String serviceLinkagePrefix, datasetMetadataPrefix;
		
	public MetadataInfoProcessor(ActorRef initiator, ActorRef metadataSource, ActorRef metadataTarget, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		this.initiator = initiator;
		this.metadataSource = metadataSource;
		this.metadataTarget = metadataTarget;
		this.serviceLinkagePrefix = serviceLinkagePrefix;
		this.datasetMetadataPrefix = datasetMetadataPrefix;
	}
	
	public static Props props(ActorRef initiator, ActorRef metadataSource, ActorRef metadataTarget, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		return Props.create(
			MetadataInfoProcessor.class, 
			Objects.requireNonNull(initiator, "initiator must not be null"), 
			Objects.requireNonNull(metadataSource, "metadataSource must not be null"),
			Objects.requireNonNull(metadataTarget, "metadataTarget must not be null"),
			Objects.requireNonNull(serviceLinkagePrefix, "serviceLinkagePrefix must not be null"),
			Objects.requireNonNull(datasetMetadataPrefix, "datasetMetadataPrefix must not be null"));
	}
	
	@Override
	public final void preStart() {		
		getContext().setReceiveTimeout(Duration.create(10, TimeUnit.SECONDS));
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
					
					ActorRef generator = getContext().actorOf(
						ServiceMetadataGenerator.props(
							metadataTarget, 
							serviceInfo, 
							serviceLinkagePrefix,
							datasetMetadataPrefix),
						nameGenerator.getName(ServiceMetadataGenerator.class));
					getContext().watch(generator);

					metadataSource.tell(
						new GetServiceMetadata(serviceId),						
						generator);
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
					
					ActorRef generator = getContext().actorOf(
						DatasetMetadataGenerator.props(
							metadataTarget, 
							currentDataset,
							serviceLinkagePrefix,
							datasetMetadataPrefix),
						nameGenerator.getName(DatasetMetadataGenerator.class));
					getContext().watch(generator);
					
					metadataSource.tell(
						new GetDatasetMetadata(currentDataset.getDataSourceId(), currentDataset.getExternalDatasetId()),
						generator);
				} else {
					log.debug("all datasets processed");
					
					log.debug("traversing services");
					
					getContext().become(
						traversingServices(
							metadataInfo, 
							metadataInfo.getServices()));
					
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
						metadataInfo.getDatasets()));
				
			metadataTarget.tell(new BeginMetadataUpdate(), getSelf());
		} else if(msg instanceof Failure) {
			log.error("failure: {}", msg);
			terminate();
		} else {
			unhandled(msg);
		}
	}

	private void terminate() {
		log.debug("terminating");
		
		metadataTarget.tell(new CommitMetadata(), initiator);
		getContext().stop(getSelf());
	}	

}
