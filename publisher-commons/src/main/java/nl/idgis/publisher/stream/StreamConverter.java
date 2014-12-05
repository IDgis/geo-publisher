package nl.idgis.publisher.stream;

import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class StreamConverter<T extends Item> extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final ActorRef target;
	
	private ActorRef producer;
	
	public StreamConverter(ActorRef target) {		
		this.target = target;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof End) {
			log.debug("end");
			
			target.tell(msg, getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Item) {
			log.debug("item");
			
			producer = getSender();
			convert((Item)msg);
		} else if(msg instanceof NextItem) {
			log.debug("next item");
			
			producer.tell(msg, getSelf());
		} else if(msg instanceof Stop) {
			log.debug("stop");
			
			producer.tell(msg, getSelf());
			getContext().stop(getSelf());
		}
	}
	
	protected abstract void convert(Item msg) throws Exception;
	
}
