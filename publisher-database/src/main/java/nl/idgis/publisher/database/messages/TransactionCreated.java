package nl.idgis.publisher.database.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class TransactionCreated implements Serializable {
	
	private static final long serialVersionUID = 3253597771471734976L;
	
	private final ActorRef actor;	

	public TransactionCreated(ActorRef actor) {
		this.actor = actor;
	}

	public ActorRef getActor() {
		return actor;
	}

	@Override
	public String toString() {
		return "TransactionCreated [actor=" + actor + "]";
	}
}
