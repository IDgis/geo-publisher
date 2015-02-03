package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class DataSourceConnected implements Serializable {
	
	private static final long serialVersionUID = -3463655338032062664L;

	private final String dataSourceId;
	
	private final ActorRef dataSource;
	
	public DataSourceConnected(String dataSourceId, ActorRef dataSource) {
		this.dataSourceId = dataSourceId;
		this.dataSource = dataSource;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public ActorRef getDataSource() {
		return dataSource;
	}

	@Override
	public String toString() {
		return "DataSourceConnected [dataSourceId=" + dataSourceId
				+ ", dataSource=" + dataSource + "]";
	}
}
