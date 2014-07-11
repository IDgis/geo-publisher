package nl.idgis.publisher.protocol.stream;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public abstract class StreamProvider<T, U extends Start, V extends Item> extends UntypedActor {
	
	private final Class<? extends StreamCursor<T, V>> cursorClass;
	
	protected StreamProvider(Class<? extends StreamCursor<T, V>> cursorClass) {
		this.cursorClass = cursorClass;
	}

	protected abstract T start(U msg) throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Start) {
			try {			
				ActorRef cursor = getContext().actorOf(Props.create(cursorClass, start((U) msg)));
				cursor.tell(new NextItem(), getSender());
			} catch(Exception e) {
				getSender().tell(new Failure(e), getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
}
