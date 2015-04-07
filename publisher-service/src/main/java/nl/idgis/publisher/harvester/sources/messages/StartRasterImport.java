package nl.idgis.publisher.harvester.sources.messages;

import akka.actor.ActorRef;

public class StartRasterImport extends StartImport {

	private static final long serialVersionUID = 2949184758378577604L;
	
	private final long size;	
	
	public StartRasterImport(ActorRef initiator, long size) {
		super(initiator);
		
		this.size = size;
	}	

	public long getSize() {
		return size;
	}

	@Override
	public String toString() {
		return "StartRasterImport [initiator=" + initiator + ", size=" + size
				+ "]";
	}
}
