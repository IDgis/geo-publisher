package nl.idgis.publisher.protocol;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Registered implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final ActorRef messagePackagerProvider;
	
	public Registered(ActorRef messagePackagerProvider) {
		this.messagePackagerProvider = messagePackagerProvider;
	}

	public ActorRef getMessagePackagerProvider() {
		return messagePackagerProvider;
	}
}
