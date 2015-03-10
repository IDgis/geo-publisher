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

import nl.idgis.publisher.domain.Failure;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudResponse;

import nl.idgis.publisher.admin.messages.EventCompleted;
import nl.idgis.publisher.admin.messages.EventFailed;
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
					log.error("timeout while waiting for response");
					getContext().stop(getSelf());
				} else if(msg instanceof Response) {
					log.debug("response received");
					
					Object listenerResponse;
					Response<?> response = (Response<?>)msg;
					if(response.getOperationResponse().equals(CrudResponse.OK)) {
						log.debug("ok");
						listenerResponse = new EventCompleted<>(response.getValue());
					} else {
						log.debug("nok");
						listenerResponse = new EventFailed();
					}
					
					origSender.tell(msg, getSender());
					
					for(ActorRef listener : listeners) {							
						listener.tell(listenerResponse, getSelf());
					}
					
					getContext().stop(getSelf());
				} else if(msg instanceof Failure) {
					log.debug("failure received");
					
					origSender.tell(msg, getSender());
					
					for(ActorRef listener : listeners) {							
						listener.tell(new EventFailed(), getSelf());
					}
				} else {
					log.debug("unhandled: {}", msg);
					
					unhandled(msg);
				}
			}
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout while waiting for listeners");
			
			handler.tell(origMsg, origSender);
			getContext().stop(getSelf());
		} else if(msg instanceof EventWaiting || msg instanceof EventFailed) {
			ActorRef listener = getSender();
			
			log.debug("listener ready: {}", listener);
			
			pendingListeners.remove(listener);
			
			if(msg instanceof EventFailed) {
				listeners.remove(listener);
			}
			
			if(pendingListeners.isEmpty()) {
				log.debug("listeners completed before function");
				
				handler.tell(origMsg, getSelf());
				getContext().become(waitingForAnswer());
			}
		} else {
			unhandled(msg);
		}
	}

}
