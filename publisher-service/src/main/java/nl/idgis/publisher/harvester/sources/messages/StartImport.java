package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class StartImport implements Serializable {

	private static final long serialVersionUID = -5582569392322992810L;
	
	private final ActorRef initiator;
	private final long count;
	
	public StartImport(ActorRef initiator, long count) {
		this.initiator = initiator;
		this.count = count;
	}
	
	public ActorRef getInitiator() {
		return initiator;
	}
	
	public long getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "StartImport [initiator=" + initiator + ", count=" + count + "]";
	}
	
}
