package nl.idgis.publisher;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActorWithStash;
import akka.japi.Procedure;

public abstract class AbstractStateMachine<T> extends UntypedActorWithStash {
		
	private final FiniteDuration timeoutDuration;
	
	protected AbstractStateMachine() {
		this(Duration.create(15,  TimeUnit.SECONDS));
	}
	
	protected AbstractStateMachine(FiniteDuration timeout) {
		this.timeoutDuration = timeout;
	}	
	
	@Override
	public final void preStart() {
		getContext().setReceiveTimeout(timeoutDuration);
	}
	
	protected abstract void timeout(T state);
	
	protected void become(T state, Procedure<Object> behavior) {
		become(state, behavior, true);
	}
	
	protected void become(final T state, final Procedure<Object> behavior, boolean discardOld) {
		
		getContext().become(new Procedure<Object>() {			
			
			@Override
			public void apply(Object msg) throws Exception {
				
				if(msg instanceof ReceiveTimeout) {
					timeout(state);
					
					getContext().stop(getSelf());
				} else {
					behavior.apply(msg);
				}
			}
			
		}, discardOld);
	}
}
