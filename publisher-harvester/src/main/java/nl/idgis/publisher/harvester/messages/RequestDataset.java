package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class RequestDataset implements Serializable {
	
	private static final long serialVersionUID = -8123020355268917516L;
	
	private final String dataSourceId, sourceDatasetId;	
	private final ActorRef sink;
	
	public RequestDataset(String dataSourceId, String sourceDatasetId, ActorRef sink) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
		this.sink = sink;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}

	public ActorRef getSink() {
		return sink;
	}
}
