package nl.idgis.publisher.provider.metadata.messages;

import java.io.Serializable;

public class MetadataNotFound implements Serializable {
	
	private static final long serialVersionUID = -8887721093422317030L;
	
	private final String identification;
	
	public MetadataNotFound(String identification) {
		this.identification = identification;
	}
	
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "MetadataNotFound [identification=" + identification + "]";
	}
}
