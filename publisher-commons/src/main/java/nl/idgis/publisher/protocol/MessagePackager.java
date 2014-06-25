package nl.idgis.publisher.protocol;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class MessagePackager extends UntypedActor {
	
	private final String targetName, sourceName;
	private final ActorRef handler;

	public MessagePackager(String targetName, String sourceName, ActorRef handler) {
		this.targetName = targetName;
		this.sourceName = sourceName;
		this.handler = handler;
	}
	
	public static Props props(String targetName, String sourceName, ActorRef handler) {
		return Props.create(MessagePackager.class, targetName, sourceName, handler);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		handler.tell(new Message(targetName, msg, sourceName), getSelf());
	}
}
