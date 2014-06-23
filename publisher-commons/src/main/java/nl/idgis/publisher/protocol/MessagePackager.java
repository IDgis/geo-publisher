package nl.idgis.publisher.protocol;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MessagePackager extends UntypedActor {
	
	private final String targetName;
	private final ActorRef handler;

	public MessagePackager(String targetName, ActorRef handler) {
		this.targetName = targetName;
		this.handler = handler;
	}
	
	public static Props props(String targetName, ActorRef handler) {
		return Props.create(MessagePackager.class, targetName, handler);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		handler.tell(new Message(targetName, msg), getSender());
	}
}
