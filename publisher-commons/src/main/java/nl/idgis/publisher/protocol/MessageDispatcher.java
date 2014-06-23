package nl.idgis.publisher.protocol;

import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MessageDispatcher extends UntypedActor {
	
	private final Map<String, ActorRef> targets;
	
	public MessageDispatcher(Map<String, ActorRef> targets) {
		this.targets = targets;
	}
	
	public static Props props(Map<String, ActorRef> targets) {
		return Props.create(MessageDispatcher.class, targets);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Message) {
			final Message message = (Message)msg;
			final String targetName = message.getTargetName();
			if(targets.containsKey(targetName)) {
				targets.get(targetName).tell(message.getContent(), null);
			} else {
				throw new IllegalArgumentException("Unknown target: " + targetName);
			}
		} else {
			unhandled(msg);
		}
	}

}
