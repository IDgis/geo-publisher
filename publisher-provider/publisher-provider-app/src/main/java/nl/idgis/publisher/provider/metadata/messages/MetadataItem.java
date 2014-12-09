package nl.idgis.publisher.provider.metadata.messages;

import nl.idgis.publisher.stream.messages.Item;

public class MetadataItem extends Item {	

	private static final long serialVersionUID = -9046319680590366968L;
	
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
