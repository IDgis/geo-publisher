package nl.idgis.publisher.metadata.messages;

import akka.actor.ActorRef;

public class AddDataSource {

	private final String dataSourceId;
	
	private final ActorRef actorRef;

	public AddDataSource(String dataSourceId, ActorRef actorRef) {
		this.dataSourceId = dataSourceId;
		this.actorRef = actorRef;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public ActorRef getActorRef() {
		return actorRef;
	}
}
