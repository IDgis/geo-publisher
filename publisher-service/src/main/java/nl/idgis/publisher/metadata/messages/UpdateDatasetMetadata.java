package nl.idgis.publisher.metadata.messages;

import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataTarget;

/**
 * Request {@link MetadataTarget} to update a specific dataset metadata document.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
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
