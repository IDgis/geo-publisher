package nl.idgis.publisher.metadata;

import java.util.HashMap;
import java.util.Map;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.sources.messages.GetMetadata;
import nl.idgis.publisher.metadata.messages.AddMetadataDocument;
import nl.idgis.publisher.protocol.messages.Ack;

public class DataSourceMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private Map<String, MetadataDocument> metadataDocuments;
	
	public static Props props() {
		return Props.create(DataSourceMock.class);
	}
	
	@Override
	public void preStart() {
		metadataDocuments = new HashMap<>();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof AddMetadataDocument) {
			log.debug("metadata document added");
			
			AddMetadataDocument addMetadataDocument = (AddMetadataDocument)msg;
			metadataDocuments.put(addMetadataDocument.getDatasetId(), addMetadataDocument.getMetadataDocument());
			
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof GetMetadata) {
			log.debug("metadata document requested");
			
			String datasetId = ((GetMetadata) msg).getDatasetId();
			
			if(metadataDocuments.containsKey(datasetId)) {
				getSender().tell(metadataDocuments.get(datasetId), getSelf());
			} else {
				log.error("unknown datasetId: {}", datasetId);
			}
		} else {
			unhandled(msg);
		}
	}
	
}
