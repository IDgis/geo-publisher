package nl.idgis.publisher.protocol.stream;

import nl.idgis.publisher.protocol.Failure;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public abstract class StreamProvider<T extends Start, U extends Item> extends UntypedActor {

	protected abstract Props start(T msg) throws Exception;
	
	@Override
	@SuppressWarnings("unchecked")
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Start) {
			try {			
				ActorRef cursor = getContext().actorOf(start((T) msg));
				cursor.tell(new NextItem(), getSender());
			} catch(Exception e) {
				getSender().tell(new Failure(e), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
}
