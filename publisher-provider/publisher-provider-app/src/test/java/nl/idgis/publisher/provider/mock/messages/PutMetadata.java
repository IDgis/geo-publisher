package nl.idgis.publisher.provider.mock.messages;

import java.io.Serializable;

public class PutMetadata implements Serializable {
	
	private static final long serialVersionUID = -428139069960332557L;
	
	private final String identification;
	private final byte[] content;
	
	public PutMetadata(String identification, byte[] content) {
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
		return "PutMetadata [identification=" + identification + ", content.length="
				+ content.length + "]";
	}
}
