package nl.idgis.publisher.service.geoserver.rest;

public class MetadataLink {

	private final String type, metadataType, content;
	
	public MetadataLink(String type, String metadataType, String content) {
		this.type = type;
		this.metadataType = metadataType;
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public String getMetadataType() {
		return metadataType;
	}

	public String getContent() {
		return content;
	}
	
}
