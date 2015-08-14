package nl.idgis.publisher.metadata.messages;

public abstract class MetadataItemInfo {
	
	private final String id; 
	
	MetadataItemInfo(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
}
