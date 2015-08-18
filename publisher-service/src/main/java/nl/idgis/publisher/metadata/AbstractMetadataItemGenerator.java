package nl.idgis.publisher.metadata;

import java.util.List;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.metadata.messages.MetadataItemInfo;
import nl.idgis.publisher.metadata.messages.PutMetadata;
import nl.idgis.publisher.stream.messages.NextItem;

public abstract class AbstractMetadataItemGenerator<T extends MetadataItemInfo, U extends PutMetadata> extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef metadataTarget;
	
	protected final T itemInfo;
	
	AbstractMetadataItemGenerator(ActorRef metadataTarget, T itemInfo) {
		this.metadataTarget = metadataTarget;
		this.itemInfo = itemInfo;
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(5, TimeUnit.SECONDS));
	}
	
	protected abstract List<? extends PutMetadata> generateMetadata(MetadataDocument metadataDocument) throws Exception;

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("message received while generating metadata document: {}", msg);
		
		if(msg instanceof MetadataDocument) {
			log.debug("metadata document received for item: {}", itemInfo.getId());
			
			generateMetadata((MetadataDocument)msg).forEach(putMetadata ->
				metadataTarget.tell(putMetadata, getSelf()));
		} else {
			log.debug("terminating");
			
			getContext().parent().tell(new NextItem(), getSelf());
			getContext().stop(getSelf());
		}
	}

}
