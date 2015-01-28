package nl.idgis.publisher.recorder.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Created implements Serializable {
	
	private static final long serialVersionUID = -1671895611955181254L;
	
	private final ActorRef actorRef;
	
	public Created(ActorRef actorRef) {
		this.actorRef = actorRef;
	}

	public ActorRef getActorRef() {
		return actorRef;
	}

	@Override
	public String toString() {
		return "Created [actorRef=" + actorRef + "]";
	}
}
