package nl.idgis.publisher.utils;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Initiator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String path;
	private final FiniteDuration interval;
	private final Object message;
	
	public Initiator(String path, FiniteDuration interval, Object message) {
		this.path = path;
		this.interval = interval;
		this.message = message;
	}
	
	public static Props props(String path, FiniteDuration interval, Object message) {
		return Props.create(Initiator.class, path, interval, message);
	}
	
	@Override
	public void preStart() {
		ActorSystem system = getContext().system();
		system.scheduler().schedule(Duration.Zero(), interval, getSelf(), message, system.dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("initiating action");
		
		getContext().actorSelection(path).tell(msg, getSelf());
	}
}
