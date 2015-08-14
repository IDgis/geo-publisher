package nl.idgis.publisher.metadata.messages;

import java.io.Serializable;

public abstract class MetadataItemInfo implements Serializable {	

	private static final long serialVersionUID = -388340809631368304L;
	
	private final String id; 
	
	MetadataItemInfo(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
}
