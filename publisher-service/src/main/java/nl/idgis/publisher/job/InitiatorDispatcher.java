package nl.idgis.publisher.job;

import java.util.Iterator;

import nl.idgis.publisher.protocol.messages.Ack;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class InitiatorDispatcher extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef target;
	
	public InitiatorDispatcher(ActorRef target) {		
		this.target = target;
	}
	
	public static Props props(ActorRef target) {
		return Props.create(InitiatorDispatcher.class, target);
	}
	
	private Procedure<Object> consuming(final Iterator<?> itr) {
		return new Procedure<Object>() {
			
			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					if(!next(itr)) {
						getContext().unbecome();
					}
				} else {
					unhandled(msg);
				}
			}
			
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Iterable) {
			log.debug("new iterable received: " + msg + " for: " + target);
			
			Iterator<?> itr = ((Iterable<?>) msg).iterator();
			if(next(itr)) {
				getContext().become(consuming(itr));
			}
		} else {
			unhandled(msg);
		}
	}

	private boolean next(Iterator<?> itr) {
		if(itr.hasNext()) {
			log.debug("sending message to target");
			
			target.tell(itr.next(), getSelf());
			
			return true;
		} else {
			log.debug("dispatching finished");
			
			getContext().parent().tell(new Ack(), getSelf());
			
			return false;
		}
	}
}
