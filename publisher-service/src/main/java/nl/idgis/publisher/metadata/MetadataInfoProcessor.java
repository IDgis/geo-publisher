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

/**
 * This actor is responsible for processing a single 
 * {@link MetadataInfo} message for a specific environment.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
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
	
	/**
	 * Creates a {@link Props} for the {@link MetadataInfoProcessor} actor.
	 * 
	 * @param initiator a reference to the actor to be notified when processing is finished.
	 * @param metadataSource a reference to the metadata source actor.
	 * @param metadataTarget a reference to the metadata target actor.
	 * @param serviceLinkagePrefix the service linkage url prefix.
	 * @param datasetMetadataPrefix the dataset url prefix.
	 * @return the props.
	 */
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
	
	/**
	 * Provides behavior for traversal of all {@link ServiceInfo} objects provided by 
	 * the received {@link MetadataInfo} object.
	 * 
	 * @param metadataInfo the received metadata info message.
	 * @param serviceItr the iterator providing the service info. 
	 * @return the new actor behavior.
	 */
	private Procedure<Object> traversingServices(
		MetadataInfo metadataInfo,
		Iterator<ServiceInfo> serviceItr) {
		
		return new Procedure<Object>() {
			
			ServiceInfo serviceInfo;

			/**
			 * Traversing services behavior. Fetches next {@link ServiceInfo} item 
			 * when receiving a message (of any type) and dispatches the {@link ServiceInfo}
			 * to a newly created {@link ServiceMetadataGenerator} actor.
			 * 
			 *@param msg the received message.
			 */
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
					
					stop();
				}
			}
			
		};
	}			
	
	/**
	 * Provides behavior for traversal of all {@link DatasetInfo} objects provided by 
	 * the received {@link MetadataInfo} object.
	 * 
	 * @param metadataInfo the received metadata info message.
	 * @param datasetItr the iterator providing the dataset info. 
	 * @return the new actor behavior.
	 */
	private Procedure<Object> traversingDatasets(
		MetadataInfo metadataInfo,
		Iterator<DatasetInfo> datasetItr) {
		
		return new Procedure<Object>() {

			DatasetInfo currentDataset;

			/**
			 * Traversing datasets behavior. Fetches next {@link DatasetInfo} item 
			 * when receiving a message (of any type) and dispatches the {@link DatasetInfo}
			 * to a newly created {@link DatasetMetadataGenerator} actor.
			 * 
			 *@param msg the received message.
			 */
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
	
	/**
	 * Default actor behavior. Handles {@link MetadataInfo} and {@link Failure}.
	 * @param msg the received message.
	 */
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof MetadataInfo) {
			log.debug("metadata info received");
			
			MetadataInfo metadataInfo = (MetadataInfo)msg;
			
			getContext().become(
					traversingDatasets(
						metadataInfo,
						metadataInfo.getDatasets()));
				
			// start metadata update session -> expected answer: Ack
			metadataTarget.tell(new BeginMetadataUpdate(), getSelf());
		} else if(msg instanceof Failure) {
			log.error("failure: {}", msg);
			stop();
		} else {
			unhandled(msg);
		}
	}

	
	/**
	 * Stops actor and requests metadata target to commit metadata.
	 */
	private void stop() {
		log.debug("terminating");
		
		metadataTarget.tell(new CommitMetadata(), initiator);
		getContext().stop(getSelf());
	}

}
