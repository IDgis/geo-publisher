package nl.idgis.publisher.protocol.metadata;

import java.io.Serializable;

public class Item implements Serializable {
	
	private static final long serialVersionUID = 8571729379042373915L;
	
	public final String identification;
	
	public Item(String identification) {
		this.identification = identification;
	}
	
	public String getIdentification() {
		return identification;
	}

	@Override
	public String toString() {
		return "Item [identification=" + identification + "]";
	}
}
