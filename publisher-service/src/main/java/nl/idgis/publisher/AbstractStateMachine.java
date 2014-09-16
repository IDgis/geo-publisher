package nl.idgis.publisher;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.messages.Timeout;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.Cancellable;
import akka.actor.UntypedActor;
import akka.japi.Procedure;

public abstract class AbstractStateMachine<T> extends UntypedActor {
		
	private final FiniteDuration timeoutDuration;
	
	private Cancellable timeoutCancellable = null;
	
	protected AbstractStateMachine() {
		this(Duration.create(15,  TimeUnit.SECONDS));
	}
	
	protected AbstractStateMachine(FiniteDuration timeout) {
		this.timeoutDuration = timeout;
	}

	protected void scheduleTimeout() {
		if(timeoutCancellable != null ) {
			timeoutCancellable.cancel();
		}
		
		timeoutCancellable = getContext().system().scheduler()
				.scheduleOnce(					 
					timeoutDuration, 
					
					getSelf(), new Timeout(), 
					
					getContext().dispatcher(), getSelf());
	}
	
	protected abstract void timeout(T state);
	
	protected void become(final T state, final Procedure<Object> behavior) {
		
		scheduleTimeout();
		
		getContext().become(new Procedure<Object>() {			
			
			@Override
			public void apply(Object msg) throws Exception {
				scheduleTimeout();
				
				if(msg instanceof Timeout) {
					timeout(state);
					
					getContext().stop(getSelf());
				} else {
					behavior.apply(msg);
				}
			}
			
		});
	}
}
