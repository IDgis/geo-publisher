package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;

public abstract class PutMetadata implements Serializable {

	private static final long serialVersionUID = -2170474224756188882L;
	
	protected final MetadataDocument metadataDocument;
	
	public PutMetadata(MetadataDocument metadataDocument) {
		this.metadataDocument = Objects.requireNonNull(metadataDocument, "metadataDocument must not be null");
	}

	public MetadataDocument getMetadataDocument() {
		return metadataDocument;
	}

}
