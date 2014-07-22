package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

public class Dataset implements Serializable {
	
	private static final long serialVersionUID = 2092372442747258535L;
	
	private final String id;
	
	public Dataset(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + "]";
	}
}
