package nl.idgis.publisher.provider.metadata.messages;

import java.io.Serializable;

public class GetMetadataId implements Serializable {

	private static final long serialVersionUID = -8984162285168147006L;
	
	private final String datasetId;
	
	public GetMetadataId(String datasetId) {
		this.datasetId = datasetId;
	}

	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "GetMetadataId [datasetId=" + datasetId + "]";
	}
}
