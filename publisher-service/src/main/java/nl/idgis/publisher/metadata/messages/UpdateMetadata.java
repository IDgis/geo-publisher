package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;

public abstract class UpdateMetadata implements Serializable {	

	private static final long serialVersionUID = 6124312154111301223L;
	
	protected final MetadataDocument metadataDocument;
	
	public UpdateMetadata(MetadataDocument metadataDocument) {
		this.metadataDocument = Objects.requireNonNull(metadataDocument, "metadataDocument must not be null");
	}

	public MetadataDocument getMetadataDocument() {
		return metadataDocument;
	}

}
