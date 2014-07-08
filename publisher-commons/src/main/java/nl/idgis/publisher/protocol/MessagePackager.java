package nl.idgis.publisher.protocol;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class MessagePackager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String targetName;
	private final ActorRef messageTarget;

	public MessagePackager(String targetName, ActorRef messageTarget) {
		this.targetName = targetName;		
		this.messageTarget = messageTarget;
	}
	
	public static Props props(String targetName, ActorRef messageTarget) {
		return Props.create(MessagePackager.class, targetName, messageTarget);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		final String sourceName;
		
		ActorRef sender = getSender();
		if(sender.equals(getContext().system().deadLetters())) {
			log.debug("no sender");			
			sourceName = null;
		} else {			
			log.debug("sender: " + sender);
			sourceName = sender.path().name();			
		}
		
		messageTarget.tell(new Message(targetName, msg, sourceName), getSelf());
	}
}
