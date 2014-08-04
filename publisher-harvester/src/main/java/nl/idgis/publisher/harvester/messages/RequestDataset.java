package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

import akka.actor.Props;

public class RequestDataset implements Serializable {
	
	private static final long serialVersionUID = -8123020355268917516L;
	
	private final String datasetId, dataSourceId, sourceDatasetId;
	private final Props receiverProps;
	
	public RequestDataset(String datasetId, String dataSourceId, String sourceDatasetId, Props receiverProps) {
		this.datasetId = datasetId;
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
		this.receiverProps = receiverProps;
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}
	
	public Props getReceiverProps() {
		return receiverProps;
	}
}
