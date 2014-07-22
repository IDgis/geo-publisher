package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

public class Dataset implements Serializable {

	private static final long serialVersionUID = -230416219187079578L;
	
	private final String id, name;
	
	public Dataset(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + ", name=" + name + "]";
	}
	
}
