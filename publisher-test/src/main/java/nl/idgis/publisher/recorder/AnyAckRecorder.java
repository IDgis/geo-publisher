package nl.idgis.publisher.recorder;

import akka.actor.ActorRef;
import akka.actor.Props;

public class AnyAckRecorder extends AnyRecorder {
	
	private final Object ack;
	
	public AnyAckRecorder(Object ack) {
		this.ack = ack;
	}
	
	public static Props props(Object ack) {
		return Props.create(AnyAckRecorder.class, ack);
	}

	@Override
	protected void onRecord(Object msg, ActorRef sender) {
		sender.tell(ack, getSelf());
	}
}
