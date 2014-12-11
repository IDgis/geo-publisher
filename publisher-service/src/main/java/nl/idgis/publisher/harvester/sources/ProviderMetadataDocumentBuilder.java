package nl.idgis.publisher.harvester.sources;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.DatasetInfo;

public class ProviderMetadataDocumentBuilder extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Duration timeout = Duration.create(15, TimeUnit.SECONDS);
	
	private final ActorRef sender;
	
	public ProviderMetadataDocumentBuilder(ActorRef sender) {
		this.sender = sender;
	}
	
	public static Props props(ActorRef sender) {
		return Props.create(ProviderMetadataDocumentBuilder.class, sender);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(timeout);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout");
			
			getContext().stop(getSelf());
		} else if(msg instanceof DatasetInfo) {
			log.debug("dataset info received");
			
			for(Attachment attachment : ((DatasetInfo) msg).getAttachments()) {
				if(attachment.getAttachmentType().equals(AttachmentType.METADATA)) {
					log.debug("metadata document found");
					
					MetadataDocumentFactory metadataDocumentFactory = new MetadataDocumentFactory();
					MetadataDocument metadataDocument = metadataDocumentFactory.parseDocument((byte[])attachment.getContent());
					
					sender.tell(metadataDocument, getSelf());
					
					break;
				}
			}
			
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}

}
