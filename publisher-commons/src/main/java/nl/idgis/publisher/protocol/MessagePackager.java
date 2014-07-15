package nl.idgis.publisher.protocol;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MessagePackager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String targetName, pathPrefix;
	private final ActorRef messageTarget;
	
	private Cancellable deadWatch = null;

	public MessagePackager(String targetName, ActorRef messageTarget, String pathPrefix) {
		this.targetName = targetName;		
		this.messageTarget = messageTarget;
		this.pathPrefix = pathPrefix;
	}
	
	public static Props props(String targetName, ActorRef messageTarget, String pathPrefix) {
		return Props.create(MessagePackager.class, targetName, messageTarget, pathPrefix);
	}
	
	@Override
	public void preStart() {
		deadWatch();
	}
	
	private void deadWatch() {
		if(deadWatch != null) {
			deadWatch.cancel();
		}
		
		ActorSystem system = getContext().system();
		 
		FiniteDuration duration = Duration.create(15, TimeUnit.SECONDS);
		deadWatch = system.scheduler().schedule(duration, duration, messageTarget,
			new IsAlive(targetName), system.dispatcher(), ActorRef.noSender());  
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		deadWatch();
		
		final String sourceName;
		
		ActorRef sender = getSender();
		if(sender.equals(getContext().system().deadLetters())) {
			log.debug("no sender");			
			sourceName = null;
		} else {			
			log.debug("sender: " + sender);
			String sourcePath = sender.path().toString();
			if(sourcePath.startsWith(pathPrefix)) {
				sourceName = sourcePath.substring(pathPrefix.length() + 1);
			} else {
				log.debug("sourcePath: " + sourcePath + " pathPrefix: " + pathPrefix);
				throw new IllegalStateException("sender is not a child of container actor");
			}
		}
		
		messageTarget.tell(new Envelope(targetName, msg, sourceName), getSelf());
	}
	
	@Override
	public void postStop() {
		if(deadWatch != null) {
			deadWatch.cancel();
		}
	}
}
