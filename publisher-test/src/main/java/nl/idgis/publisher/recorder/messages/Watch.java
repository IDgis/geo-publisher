package nl.idgis.publisher.recorder.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Watch implements Serializable {

	private static final long serialVersionUID = -6383256480102910816L;
	
	private final ActorRef actorRef;
	
	public Watch(ActorRef actorRef) {
		this.actorRef = actorRef;
	}

	public ActorRef getActorRef() {
		return actorRef;
	}

	@Override
	public String toString() {
		return "Watch [actorRef=" + actorRef + "]";
	}
}
