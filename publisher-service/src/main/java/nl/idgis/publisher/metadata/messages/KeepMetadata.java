package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

public class KeepMetadata implements Serializable {

	private static final long serialVersionUID = 4772707371455361882L;

	private final MetadataType type;
	
	private final String id;
	
	public KeepMetadata(MetadataType type, String id) {
		this.type = type;
		this.id = id;
	}
	
	public MetadataType getType() {
		return type;
	}
	
	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "KeepMetadata [type=" + type + ", id=" + id + "]";
	}
}
