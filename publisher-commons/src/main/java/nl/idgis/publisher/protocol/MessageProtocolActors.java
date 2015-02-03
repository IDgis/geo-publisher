package nl.idgis.publisher.protocol;

import java.io.Serializable;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.protocol.messages.ListenerInit;
import nl.idgis.publisher.protocol.messages.Register;
import nl.idgis.publisher.protocol.messages.Registered;
import nl.idgis.publisher.utils.FutureUtils;

public abstract class MessageProtocolActors extends UntypedActor {
	
	private static class CreateActors implements Serializable {
	
		private static final long serialVersionUID = -5194402567506379192L;
		
		private final ActorRef messagePackagerProvider;
		
		CreateActors(ActorRef messagePackagerProvider) {
			this.messagePackagerProvider = messagePackagerProvider;
		}
		
		ActorRef getMessagePackagerProvider() {
			return messagePackagerProvider;
		}

		@Override
		public String toString() {
			return "CreateActors [messagePackagerProvider="
					+ messagePackagerProvider + "]";
		}
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected abstract void createActors(ActorRef messagePackagerProvider);
	
	protected void onReceiveElse(Object msg) {
		unhandled(msg);
	}
	
	protected FutureUtils f;
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher());
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof ListenerInit) {
			ActorRef self = getSelf(), sender = getSender();	
			
			ActorRef messageProtocolHandler = ((ListenerInit)msg).getMessagePackagerProvider();
			f.ask(messageProtocolHandler, new Register(getSelf()), Registered.class).whenComplete((registered, t) -> {
				if(t != null) {
					sender.tell(new Failure(t), self);
				} else {				
					ActorRef messagePackagerProvider = registered.getMessagePackagerProvider();
				
					log.debug("registered");
					self.tell(new CreateActors(messagePackagerProvider), self);
				}
			});
		} else if(msg instanceof CreateActors) {
			createActors(((CreateActors)msg).getMessagePackagerProvider());
		} else {
			onReceiveElse(msg);
		}
	}

}
