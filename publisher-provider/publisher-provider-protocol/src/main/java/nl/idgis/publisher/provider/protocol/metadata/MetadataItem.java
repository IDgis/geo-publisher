package nl.idgis.publisher.provider.protocol.metadata;

import java.io.Serializable;

import nl.idgis.publisher.protocol.stream.Item;

public class MetadataItem extends Item implements Serializable {
	
	private static final long serialVersionUID = 8571729379042373915L;
	
	public final String identification, title, alternateTitle;
	
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
