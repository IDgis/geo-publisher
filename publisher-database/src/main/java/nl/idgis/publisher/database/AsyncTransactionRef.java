package nl.idgis.publisher.database;

import java.io.Serializable;

import akka.actor.ActorRef;

public class AsyncTransactionRef implements Serializable {

	private static final long serialVersionUID = -6561897724657692904L;
	
	private final ActorRef actorRef;
	
	protected AsyncTransactionRef(ActorRef actorRef) {
		this.actorRef = actorRef;
	}
	
	protected ActorRef getActorRef() {
		return actorRef;
	}

	@Override
	public String toString() {
		return "AsyncTransactionRef [actorRef=" + actorRef + "]";
	}
}
