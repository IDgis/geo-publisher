package nl.idgis.publisher.protocol;

import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.protocol.ConnectionListener.ActorPair;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MessageDispatcher extends UntypedActor {

	private final Map<String, ActorRef> targets;

	private final Map<ActorPair, ActorRef> remoteRefs;

	public MessageDispatcher(Map<String, ActorRef> targets,
			Map<ActorPair, ActorRef> remoteRefs) {
		this.targets = targets;
		this.remoteRefs = new HashMap<ActorPair, ActorRef>(remoteRefs);
	}

	public static Props props(Map<String, ActorRef> targets,
			Map<ActorPair, ActorRef> remoteRefs) {
		return Props.create(MessageDispatcher.class, targets, remoteRefs);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Message) {
			final Message message = (Message) msg;
			final String targetName = message.getTargetName();
			
			ActorRef sender;
			ActorPair pair = new ActorPair(message.getTargetName(), message.getSourceName());
			if(remoteRefs.containsKey(pair)) {
				sender = remoteRefs.get(pair);
			} else {
				sender = getContext().actorOf(MessagePackager.props(message.getSourceName(), message.getTargetName(), getSender()));
				remoteRefs.put(pair, sender);
			}
			
			if (targets.containsKey(targetName)) {
				targets.get(targetName).tell(message.getContent(), sender);
			} else {
				throw new IllegalArgumentException("Unknown target: "
						+ targetName);
			}
		} else {
			unhandled(msg);
		}
	}

}
