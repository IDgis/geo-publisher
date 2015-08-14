package nl.idgis.publisher.metadata;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.metadata.messages.PutDatasetMetadata;
import nl.idgis.publisher.metadata.messages.PutServiceMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;

public class MetadataTarget extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final MetadataStore datasetMetadataStore, serviceMetadataStore;
	
	public MetadataTarget(MetadataStore datasetMetadataStore, MetadataStore serviceMetadataStore) {
		this.datasetMetadataStore = datasetMetadataStore;
		this.serviceMetadataStore = serviceMetadataStore;
	}
	
	public static Props props(MetadataStore datasetMetadataStore, MetadataStore serviceMetadataStore) {
		return Props.create(MetadataTarget.class, datasetMetadataStore, serviceMetadataStore);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof PutServiceMetadata) {
			handlePutServiceMetadata((PutServiceMetadata)msg);
		} else if(msg instanceof PutDatasetMetadata) {
			handlePutDatasetMetadata((PutDatasetMetadata)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void doPut(MetadataStore metadataStore, String name, MetadataDocument metadataDocument) {
		ActorRef sender = getSender();
		metadataStore.put(name, metadataDocument).whenComplete((v, throwable) -> {
			if(throwable == null) {
				sender.tell(new Ack(), getSelf());
			} else {
				sender.tell(new Failure(throwable), getSelf());
			}
		});
	}

	private void handlePutDatasetMetadata(PutDatasetMetadata msg) {
		String datasetId = msg.getDatasetId();
		log.debug("storing dataset metadata: {}", datasetId);		
		doPut(datasetMetadataStore, datasetId, msg.getMetadataDocument());
	}

	private void handlePutServiceMetadata(PutServiceMetadata msg) {
		String serviceId = msg.getServiceId();
		log.debug("storing service metadata: {}", serviceId);		
		doPut(serviceMetadataStore, serviceId, msg.getMetadataDocument());
	}

}
