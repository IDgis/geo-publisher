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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((metadataType == null) ? 0 : metadataType.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetadataLink other = (MetadataLink) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (metadataType == null) {
			if (other.metadataType != null)
				return false;
		} else if (!metadataType.equals(other.metadataType))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
	
	@Override
	public String toString () {
		return String.format ("%s (%s, %s)", content, type, metadataType);
	}
}
