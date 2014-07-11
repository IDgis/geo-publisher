package nl.idgis.publisher.protocol;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public abstract class MessageProtocolActors extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected abstract void createActors(ActorRef messagePackagerProvider);

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof ListenerInit) {
			ActorRef messageProtocolHandler = ((ListenerInit) msg).getMessagePackagerProvider();
			Patterns.ask(messageProtocolHandler, new Register(getSelf()), 1000).onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object o) throws Throwable {
					Registered registered = (Registered)o;
					ActorRef messagePackagerProvider = registered.getMessagePackagerProvider();
					
					log.debug("registered");
					createActors(messagePackagerProvider);
				}
				
			}, getContext().system().dispatcher());
		} else {
			unhandled(msg);
		}
	}

}
