package nl.idgis.publisher.protocol;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.protocol.messages.Envelope;
import nl.idgis.publisher.protocol.messages.SetPersistent;
import nl.idgis.publisher.protocol.messages.StopPackager;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MessagePackager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String targetName, pathPrefix;
	
	private final ActorRef messageTarget;

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
		getContext().setReceiveTimeout(Duration.apply(2, TimeUnit.MINUTES));
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.debug("timeout");			
			getContext().parent().tell(new StopPackager(targetName), getSelf());
		} else if(msg instanceof SetPersistent) {
			log.debug("set persistent");
			getContext().parent().tell(msg, getSelf());
		} else {
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
					log.error("sourcePath: " + sourcePath + " pathPrefix: " + pathPrefix + " msg: " + msg.toString());
					throw new IllegalStateException("sender is not a child of container actor");
				}
			}
			
			messageTarget.tell(new Envelope(targetName, msg, sourceName), getSelf());
		}
	}
}
