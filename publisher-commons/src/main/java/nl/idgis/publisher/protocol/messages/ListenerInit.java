package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class ListenerInit implements Serializable {
	
	private static final long serialVersionUID = -3190926701898830134L;
	
	private final ActorRef messagePackagerProvider;
	
	public ListenerInit(ActorRef messagePackagerProvider) {
		this.messagePackagerProvider = messagePackagerProvider;
	}
	
	public ActorRef getMessagePackagerProvider() {
		return messagePackagerProvider;
	}

	@Override
	public String toString() {
		return "ListenerInit [messagePackagerProvider="
				+ messagePackagerProvider + "]";
	}
}
