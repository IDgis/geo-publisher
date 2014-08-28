package nl.idgis.publisher.database.messages;

import java.io.Serializable;

public class BaseDatasetInfo implements Serializable {

	private static final long serialVersionUID = -890886490140917469L;
	
	protected final String id;
	protected final String name;

	public BaseDatasetInfo (final String id, final String name) {
		this.id = id;
		this.name = name;
	}

	public String getId () {
		return id;
	}

	public String getName () {
		return name;
	}

}