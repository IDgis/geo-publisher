package nl.idgis.publisher.protocol;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MessagePackagerProvider extends UntypedActor {
	
	private final String pathPrefix;
	private final ActorRef messageTarget;
	
	private Map<String, ActorRef> messagePackagers;
	
	private MessagePackagerProvider(ActorRef messageTarget, String pathPrefix) {
		this.messageTarget = messageTarget;
		this.pathPrefix = pathPrefix;
	}
	
	public static Props props(ActorRef messageTarget, String pathPrefix) {
		return Props.create(MessagePackagerProvider.class, messageTarget, pathPrefix);
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
				packager = getContext().actorOf(MessagePackager.props(targetName, messageTarget, pathPrefix));
				messagePackagers.put(targetName, packager);
			}
			
			getSender().tell(packager, getSelf());
		} else {
			unhandled(msg);
		}
	}
}
