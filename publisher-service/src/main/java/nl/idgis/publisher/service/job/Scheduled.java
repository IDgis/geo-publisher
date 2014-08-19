package nl.idgis.publisher.service.job;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import nl.idgis.publisher.service.job.messages.Initiate;
import akka.actor.UntypedActor;

public abstract class Scheduled extends UntypedActor {
	
	private final FiniteDuration interval;
	
	public Scheduled() {
		this(Duration.create(10, TimeUnit.SECONDS));
	}
	
	public Scheduled(FiniteDuration interval) {
		this.interval = interval;
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().system().scheduler().schedule(Duration.Zero(), interval, getSelf(), new Initiate(), getContext().dispatcher(), getSelf());
	}

	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof Initiate) {
			doInitiate();
		} else {
			unhandled(msg);
		}
	}

	protected abstract void doInitiate();
}
