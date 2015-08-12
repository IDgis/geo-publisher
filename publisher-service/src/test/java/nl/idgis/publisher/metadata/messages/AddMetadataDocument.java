package nl.idgis.publisher.metadata.messages;

import nl.idgis.publisher.metadata.MetadataDocument;

public class AddMetadataDocument {

	private final String datasetId;
	
	private final MetadataDocument metadataDocument;

	public AddMetadataDocument(String datasetId, MetadataDocument metadataDocument) {
		this.datasetId = datasetId;
		this.metadataDocument = metadataDocument;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public MetadataDocument getMetadataDocument() {
		return metadataDocument;
	}
}
