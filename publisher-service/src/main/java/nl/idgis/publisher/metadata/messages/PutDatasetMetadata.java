package nl.idgis.publisher.metadata.messages;

import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;

public class PutDatasetMetadata extends PutMetadata {		

	private static final long serialVersionUID = -8125975249047975901L;
	
	private final String datasetId;

	public PutDatasetMetadata(String datasetId, MetadataDocument metadataDocument) {
		super(metadataDocument);
		
		this.datasetId = Objects.requireNonNull(datasetId, "datasetId must not be null");
	}

	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public String toString() {
		return "PutDatasetMetadata [datasetId=" + datasetId + ", metadataDocument=" + metadataDocument + "]";
	}
}
