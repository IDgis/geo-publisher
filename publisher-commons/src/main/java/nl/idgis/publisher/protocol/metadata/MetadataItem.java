package nl.idgis.publisher.protocol.metadata;

import java.io.Serializable;

import nl.idgis.publisher.protocol.stream.Item;

public class MetadataItem extends Item implements Serializable {
	
	private static final long serialVersionUID = 8571729379042373915L;
	
	public final String identification, title;
	
	public MetadataItem(String identification, String title) {
		this.identification = identification;
		this.title = title;
	}
	
	public String getIdentification() {
		return identification;
	}
	
	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return "MetadataItem [identification=" + identification + ", title=" + title
				+ "]";
	}
}
