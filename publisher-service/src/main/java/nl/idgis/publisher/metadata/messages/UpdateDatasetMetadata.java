package nl.idgis.publisher.metadata.messages;

import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;

public class UpdateDatasetMetadata extends UpdateMetadata {
	
	private static final long serialVersionUID = 6614273320105612469L;
	
	private final String datasetId;

	public UpdateDatasetMetadata(String datasetId, MetadataDocument metadataDocument) {
		super(metadataDocument);
		
		this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
	}

	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "UpdateDatasetMetadata [datasetId=" + datasetId + ", metadataDocument=" + metadataDocument + "]";
	}
}
