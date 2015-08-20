package nl.idgis.publisher.provider.metadata.messages;

import java.io.Serializable;

public class GetDatasetId implements Serializable {
	
	private static final long serialVersionUID = -2532267982353758034L;
	
	private final String metadataId;
	
	public GetDatasetId(String metadataId) {
		this.metadataId = metadataId;
	}

	public String getMetadataId() {
		return metadataId;
	}

	@Override
	public String toString() {
		return "GetDatasetId [metadataId=" + metadataId + "]";
	}
}
