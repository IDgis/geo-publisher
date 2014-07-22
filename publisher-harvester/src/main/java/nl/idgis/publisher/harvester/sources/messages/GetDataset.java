package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class GetDataset implements Serializable {

	private static final long serialVersionUID = 8918475811419460221L;
	
	private final String id;
	private final ActorRef sink;
	
	public GetDataset(String id, ActorRef sink) {
		this.id = id;
		this.sink = sink;
	}

	public String getId() {
		return id;
	}

	public ActorRef getSink() {
		return sink;
	}

	@Override
	public String toString() {
		return "GetDataset [id=" + id + ", sink=" + sink + "]";
	}
}
