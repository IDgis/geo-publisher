package nl.idgis.publisher.harvester.sources.messages;

import akka.actor.ActorRef;

public class StartVectorImport extends StartImport {

	private static final long serialVersionUID = 5030582031553429607L;	
	
	private final long count;
	
	public StartVectorImport(ActorRef initiator, long count) {
		super(initiator);
		
		this.count = count;
	}
	
	public long getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "StartVectorImport [initiator=" + initiator + ", count=" + count + "]";
	}
	
}
