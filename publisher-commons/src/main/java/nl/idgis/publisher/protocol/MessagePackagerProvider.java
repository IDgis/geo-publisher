package nl.idgis.publisher.protocol;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MessagePackagerProvider extends UntypedActor {
	
	private final ActorRef messageTarget;
	
	private Map<String, ActorRef> messagePackagers;
	
	private MessagePackagerProvider(ActorRef messageTarget) {
		this.messageTarget = messageTarget;
	}
	
	public static Props props(ActorRef messageTarget) {
		return Props.create(MessagePackagerProvider.class, messageTarget);
	}
	
	@Override
	public void preStart() {
		messagePackagers = new HashMap<String, ActorRef>();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetMessagePackager) {
			GetMessagePackager gmp = (GetMessagePackager)msg;
			String targetName = gmp.getTargetName();
			
			final ActorRef packager;			
			if(messagePackagers.containsKey(targetName)) {
				packager = messagePackagers.get(targetName);
			} else {
				packager = getContext().actorOf(MessagePackager.props(targetName, messageTarget));
				messagePackagers.put(targetName, packager);
			}
			
			getSender().tell(packager, getSelf());
		} else {
			unhandled(msg);
		}
	}
}
