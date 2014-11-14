package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

public class GetDatasetMetadata implements Serializable {

	private static final long serialVersionUID = -3893617546297813357L;
	
	private final String datasetId;
	
	public GetDatasetMetadata(String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "GetDatasetMetadata [datasetId=" + datasetId + "]";
	}	
}
