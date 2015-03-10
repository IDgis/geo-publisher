package nl.idgis.publisher.admin;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.Failure;

import nl.idgis.publisher.admin.messages.Event;

public class EventDispatcher extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Object origMsg;
	
	private final ActorRef origSender;
	
	private final Set<ActorRef> listeners;
	
	private final Duration timeout;
	
	public EventDispatcher(Object origMsg, ActorRef origSender, Set<ActorRef> listeners, Duration timeout) {
		this.origMsg = origMsg;
		this.origSender = origSender;
		this.listeners = listeners;
		this.timeout = timeout;
	}
	
	public static Props props(Object origMsg, ActorRef origSender, Set<ActorRef> listeners) {
		return props(origMsg, origSender, listeners, Duration.create(10, TimeUnit.SECONDS));
	}
	
	public static Props props(Object origMsg, ActorRef origSender, Set<ActorRef> listeners, Duration timeout) {
		return Props.create(EventDispatcher.class, origMsg, origSender, listeners, timeout);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(timeout);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout");
		} else {		
			log.debug("answer received");
			
			origSender.forward(msg, getContext());
			
			if(msg instanceof Failure) {
				log.debug("failure received");
			} else {
				log.debug("dispatching event messages");
				
				Event event = new Event(origMsg, msg);
				for(ActorRef listener : listeners) {
					listener.tell(event, getSelf());
				}
			}
		}
		
		getContext().stop(getSelf());
	}

}
