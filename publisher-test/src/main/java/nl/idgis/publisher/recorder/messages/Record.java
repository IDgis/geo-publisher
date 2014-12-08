package nl.idgis.publisher.recorder.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Record implements Serializable {	

	private static final long serialVersionUID = -6634355211883677212L;

	private final ActorRef self, sender;

	private final Object message;
	
	public Record(ActorRef self, ActorRef sender, Object message) {
		this.self = self;
		this.sender = sender;
		this.message = message;
	}

	public ActorRef getSelf() {
		return self;
	}

	public ActorRef getSender() {
		return sender;
	}

	public Object getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "Record [self=" + self + ", sender=" + sender + ", message="
				+ message + "]";
	}
	
}
