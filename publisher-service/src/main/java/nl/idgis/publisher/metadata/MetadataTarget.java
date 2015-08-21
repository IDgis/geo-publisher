package nl.idgis.publisher.metadata;

import java.nio.file.Files;
import java.nio.file.Path;

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
	
	private final Path serviceMetadataDirectory, datasetMetadataDirectory;
	
	public MetadataTarget(Path serviceMetadataDirectory, Path datasetMetadataDirectory) {
		this.serviceMetadataDirectory = serviceMetadataDirectory;
		this.datasetMetadataDirectory = datasetMetadataDirectory;
	}
	
	public static Props props(Path serviceMetadataDirectory, Path datasetMetadataDirectory) {
		return Props.create(MetadataTarget.class, serviceMetadataDirectory, datasetMetadataDirectory);
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
	
	private void doPut(Path metadataDirectory, String name, MetadataDocument metadataDocument) {
		try {
			Files.write(				
				metadataDirectory.resolve(name + ".xml"),
				metadataDocument.getContent());
			getSender().tell(new Ack(), getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}

	private void handlePutDatasetMetadata(PutDatasetMetadata msg) {
		String datasetId = msg.getDatasetId();
		log.debug("storing dataset metadata: {}", datasetId);		
		doPut(datasetMetadataDirectory, datasetId, msg.getMetadataDocument());
	}

	private void handlePutServiceMetadata(PutServiceMetadata msg) {
		String serviceId = msg.getServiceId();
		log.debug("storing service metadata: {}", serviceId);		
		doPut(serviceMetadataDirectory, serviceId, msg.getMetadataDocument());
	}

}
