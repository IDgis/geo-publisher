package nl.idgis.publisher.stream;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Start;
import nl.idgis.publisher.utils.NameGenerator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class StreamProvider<T extends Start> extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final NameGenerator nameGenerator = new NameGenerator();

	protected abstract Props start(T msg) throws Exception;
	
	@Override
	@SuppressWarnings("unchecked")
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Start) {
			try {			
				ActorRef cursor = getContext().actorOf(start((T) msg), nameGenerator.getName("cursor"));
				cursor.tell(new NextItem(), getSender());
			} catch(Exception e) {
				log.warning("couldn't create cursor: " + e.getMessage());
				getSender().tell(new Failure(e), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
}
