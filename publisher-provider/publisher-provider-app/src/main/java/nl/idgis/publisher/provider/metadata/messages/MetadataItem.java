package nl.idgis.publisher.provider.metadata.messages;

import java.io.Serializable;

public class MetadataItem implements Serializable {

	private static final long serialVersionUID = 6298109585578693924L;
	
	private final String identification;
	
	private final byte[] content;
	
	public MetadataItem(String identification, byte[] content) {
		this.identification = identification;
		this.content = content;
	}
	
	public String getIdentification() {
		return identification;
	}
	
	public byte[] getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "MetadataItem [identification=" + identification + ", content.length="
				+ content.length + "]";
	}	
}
