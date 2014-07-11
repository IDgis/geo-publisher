package nl.idgis.publisher.protocol;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Register implements Serializable {
	
	private static final long serialVersionUID = -8083357297751822584L;
	
	private final ActorRef messageProtocolActors;
	
	public Register(ActorRef messageProtocolActors) {
		this.messageProtocolActors = messageProtocolActors;
	}

	public ActorRef getMessageProtocolActors() {
		return messageProtocolActors;
	}
}
