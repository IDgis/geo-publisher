package nl.idgis.publisher.database.messages;

import java.io.Serializable;

public class StartTransaction implements Serializable {
	
	private static final long serialVersionUID = -2213629604284533151L;
	
	private final String origin;	

	public StartTransaction(String origin) {
		this.origin = origin;
	}

	public String getOrigin() {
		return origin;
	}
}
