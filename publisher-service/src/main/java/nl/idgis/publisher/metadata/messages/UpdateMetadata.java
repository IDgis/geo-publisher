package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;
import java.util.Objects;

import nl.idgis.publisher.metadata.MetadataDocument;

public class UpdateMetadata implements Serializable {	

	private static final long serialVersionUID = 6124312154111301223L;
	
	private final MetadataType type;
	
	private final String id;
	
	private final MetadataDocument metadataDocument;
	
	public UpdateMetadata(MetadataType type, String id, MetadataDocument metadataDocument) {
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.metadataDocument = Objects.requireNonNull(metadataDocument, "metadataDocument must not be null");
	}
	
	public MetadataType getType() {
		return type;
	}
	
	public String getId() {
		return id;
	}

	public MetadataDocument getMetadataDocument() {
		return metadataDocument;
	}

}
