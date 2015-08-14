package nl.idgis.publisher.metadata;

import java.util.concurrent.TimeUnit;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.metadata.messages.MetadataItemInfo;
import nl.idgis.publisher.stream.messages.NextItem;

public abstract class AbstractMetadataItemGenerator<T extends MetadataItemInfo> extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final T itemInfo;
	
	AbstractMetadataItemGenerator(T itemInfo) {
		this.itemInfo = itemInfo;
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(1, TimeUnit.SECONDS));
	}
	
	protected abstract void generateMetadata(MetadataDocument metadataDocument);

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("message received while waiting for metadata document: {}", msg);
		
		if(msg instanceof MetadataDocument) {
			log.debug("metadata document received for item: {}", itemInfo.getId());
			
			generateMetadata((MetadataDocument)msg);
		}
		
		getContext().parent().tell(new NextItem(), getSelf());
		getContext().stop(getSelf());
	}

}
