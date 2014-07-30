package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

public class RequestDataset implements Serializable {
	
	private static final long serialVersionUID = -8123020355268917516L;
	
	private final String dataSourceId, sourceDatasetId;
	
	public RequestDataset(String dataSourceId, String sourceDatasetId) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;		
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}
}
