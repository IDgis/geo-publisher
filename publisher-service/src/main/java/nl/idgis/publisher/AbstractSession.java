package nl.idgis.publisher;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.messages.Timeout;
import scala.concurrent.duration.Duration;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.UntypedActor;

public abstract class AbstractSession extends UntypedActor {

	private Cancellable timeoutCancellable;
	
	@Override
	public void preStart() throws Exception {
		scheduleTimeout();
	}
	
	protected void scheduleTimeout() {
		if(timeoutCancellable != null) {
			timeoutCancellable.cancel();
		}
		
		ActorSystem system = getContext().system();
		
		timeoutCancellable = system.scheduler().scheduleOnce(
				Duration.create(15, TimeUnit.SECONDS), 
				getSelf(), 
				new Timeout(), 
				system.dispatcher(), 
				getSelf());
	}
	
	@Override
	public final void postStop() {
		timeoutCancellable.cancel();
	}
}
