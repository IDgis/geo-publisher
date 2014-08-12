package nl.idgis.publisher.provider.protocol.metadata;

import nl.idgis.publisher.stream.messages.Item;

public class MetadataItem extends Item {	

	private static final long serialVersionUID = -9046319680590366968L;
	
	private final String identification, title, alternateTitle;
	
	public MetadataItem(String identification, String title, String alternateTitle) {
		this.identification = identification;
		this.title = title;
		this.alternateTitle = alternateTitle;
	}
	
	public String getIdentification() {
		return identification;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getAlternateTitle() {
		return alternateTitle;
	}

	@Override
	public String toString() {
		return "MetadataItem [identification=" + identification + ", title="
				+ title + ", alternateTitle=" + alternateTitle + "]";
	}	
}
