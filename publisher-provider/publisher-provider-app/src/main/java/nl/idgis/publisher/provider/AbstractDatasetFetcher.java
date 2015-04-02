package nl.idgis.publisher.provider;

import java.util.concurrent.TimeoutException;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.provider.metadata.messages.MetadataNotFound;
import nl.idgis.publisher.provider.protocol.AbstractGetDatasetRequest;
import nl.idgis.publisher.provider.protocol.DatasetNotFound;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class AbstractDatasetFetcher<T extends AbstractGetDatasetRequest> extends UntypedActor {

	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final ActorRef sender;
	
	protected final T request;
	
	protected AbstractDatasetFetcher(ActorRef sender, T request) {
		this.sender = sender;
		this.request = request;
	}
	
	protected abstract void handleMetadataItem(MetadataItem msg) throws Exception;
	
	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout while collecting information");
			
			sender.tell(new Failure(new TimeoutException("collecting information")), getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof MetadataNotFound) {
			log.debug("metadata not found");
			
			sender.tell(new DatasetNotFound(((MetadataNotFound)msg).getIdentification()), getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof MetadataItem) {
			log.debug("metadata item");
			
			handleMetadataItem((MetadataItem)msg);
		} else {			
			unhandled(msg);
		}
	}
}
