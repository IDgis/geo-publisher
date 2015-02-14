package nl.idgis.publisher.utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

public class EventDispatcher extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Object origMsg;
	
	private final ActorRef origSender;
	
	private final List<ActorRef> listeners;
	
	private final Duration timeout;
	
	public EventDispatcher(Object origMsg, ActorRef origSender, List<ActorRef> listeners, Duration timeout) {
		this.origMsg = origMsg;
		this.origSender = origSender;
		this.listeners = listeners;
		this.timeout = timeout;
	}
	
	public static Props props(Object origMsg, ActorRef origSender, List<ActorRef> listeners) {
		return props(origMsg, origSender, listeners, Duration.create(10, TimeUnit.SECONDS));
	}
	
	public static Props props(Object origMsg, ActorRef origSender, List<ActorRef> listeners, Duration timeout) {
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
			
			for(ActorRef listener : listeners) {
				listener.tell(origMsg, getSelf());
			}
		}
		
		getContext().stop(getSelf());
	}

}
