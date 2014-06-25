package nl.idgis.publisher.protocol.metadata;

import java.io.Serializable;

public class Item implements Serializable {
	
	private static final long serialVersionUID = 8571729379042373915L;
	
	public final String identification, title;
	
	public Item(String identification, String title) {
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
		return "Item [identification=" + identification + ", title=" + title
				+ "]";
	}
}
