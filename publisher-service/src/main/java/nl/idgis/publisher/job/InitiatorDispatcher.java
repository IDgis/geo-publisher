package nl.idgis.publisher.job;

import java.util.Iterator;

import nl.idgis.publisher.protocol.messages.Ack;

import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class InitiatorDispatcher extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final FiniteDuration timeout;
	private final ActorRef target;
	
	private Cancellable timeoutCancellable;
	
	public InitiatorDispatcher(ActorRef target, FiniteDuration timeout) {	
		this.target = target;
		this.timeout = timeout;
	}
	
	public static Props props(ActorRef target, FiniteDuration timeout) {
		return Props.create(InitiatorDispatcher.class, target, timeout);
	}
	
	@Override
	public void preStart() throws Exception {
		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Iterable) {
			log.debug("iterable received: " + msg);
			
			final Iterator<?> i = ((Iterable<?>) msg).iterator();
			
			if(i.hasNext()) {
				tellTarget(i.next());
				
				getContext().become(new Procedure<Object>() {

					@Override
					public void apply(Object msg) throws Exception {
						if(msg instanceof Ack) {
							log.debug("ack received");
							
							if(i.hasNext()) {
								tellTarget(i.next());
							} else {
								stop();
							}
						} else {
							log.debug("unhandled (waiting for Ack): " + msg);
							
							unhandled(msg);
						}
					}
					
				});
			} else {
				stop();
			}
		} else {
			log.debug("unhandled (waiting for Iterable): " + msg);
			
			unhandled(msg);
		}
	}
	
	private void tellTarget(Object msg) {
		log.debug("sending message to target: " + msg);
		
		target.tell(msg, getSelf());
		
		cancelTimeout();
		
		timeoutCancellable = getContext().system().scheduler().scheduleOnce(timeout, new Runnable() {

			@Override
			public void run() {
				log.debug("timeout");
				
				timeoutCancellable = null;
				
				stop();				
			}
			
		}, getContext().dispatcher());
	}

	private void cancelTimeout() {
		if(timeoutCancellable != null) {
			timeoutCancellable.cancel();
			timeoutCancellable = null;
		}
	}
	
	private void stop() {
		log.debug("stopping");
		
		cancelTimeout();
		
		getContext().parent().tell(new Ack(), getSelf());
		getContext().become(receive());
	}
}
