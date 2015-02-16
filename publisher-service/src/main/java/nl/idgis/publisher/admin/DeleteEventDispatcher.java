package nl.idgis.publisher.admin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudResponse;

import nl.idgis.publisher.admin.messages.EventCompleted;
import nl.idgis.publisher.admin.messages.EventWaiting;

public class DeleteEventDispatcher extends UntypedActor {
	
private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Object origMsg;
	
	private final ActorRef origSender, handler;
	
	private final Set<ActorRef> listeners, pendingListeners;
	
	private final Duration timeout;
	
	public DeleteEventDispatcher(Object origMsg, ActorRef origSender, ActorRef handler, Set<ActorRef> listeners, Duration timeout) {
		this.origMsg = origMsg;
		this.origSender = origSender;
		this.handler = handler;
		this.listeners = listeners;
		this.pendingListeners = new HashSet<>(listeners);
		this.timeout = timeout;
	}
	
	public static Props props(Object origMsg, ActorRef origSender, ActorRef handler, Set<ActorRef> listeners) {
		return props(origMsg, origSender, handler, listeners, Duration.create(10, TimeUnit.SECONDS));
	}
	
	public static Props props(Object origMsg, ActorRef origSender, ActorRef handler, Set<ActorRef> listeners, Duration timeout) {
		return Props.create(DeleteEventDispatcher.class, origMsg, origSender, handler, listeners, timeout);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(timeout);
	}
	
	private Procedure<Object> waitingForAnswer() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof ReceiveTimeout) {
					log.error("timeout");
					getContext().stop(getSelf());
				} else if(msg instanceof Response) {
					log.debug("response received");
					
					Response<?> response = (Response<?>)msg;
					if(response.getOperationResponse().equals(CrudResponse.OK)) {
						log.debug("ok");
						
						origSender.tell(msg, getSender());
						
						for(ActorRef listener : listeners) {							
							listener.tell(new EventCompleted(), getSelf());
						}
					} else {
						log.debug("nok");
					}
					
					getContext().stop(getSelf());
				} else {
					unhandled(msg);
				}
			}
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout");
			getContext().stop(getSelf());
		} else if(msg instanceof EventWaiting) {
			log.debug("waiting");
			
			pendingListeners.remove(getSender());
			
			if(pendingListeners.isEmpty()) {
				log.debug("before listeners completed");
				
				handler.tell(origMsg, getSelf());
				getContext().become(waitingForAnswer());
			}
		} else {
			unhandled(msg);
		}
	}

}
