package nl.idgis.publisher.stream;

import scala.concurrent.Future;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;

public abstract class StreamConverter extends UntypedActor {
	
	private final ActorRef target;
	
	private ActorRef producer;
	
	public StreamConverter(ActorRef target) {		
		this.target = target;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof End) {
			target.tell(msg, getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Item) {
			producer = getSelf();
			
			Patterns.pipe(convert((Item)msg), getContext().dispatcher())
				.pipeTo(target, getSelf());
		} else if(msg instanceof NextItem) {
			producer.tell(msg, getSelf());
		} else if(msg instanceof Stop) {			
			producer.tell(msg, getSelf());
			getContext().stop(getSelf());
		}
	}
	
	protected abstract Future<Item> convert(Item msg);
	
}
