package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public abstract class StartImport implements Serializable {

	private static final long serialVersionUID = -7298951026938449629L;
	
	protected final ActorRef initiator;
	
	protected StartImport(ActorRef initiator) {
		this.initiator = initiator;
	}

	public ActorRef getInitiator() {
		return initiator;
	}
}
