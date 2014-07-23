package nl.idgis.publisher.harvester.sources.messages;

import java.io.Serializable;

public class Column implements Serializable {
	
	private static final long serialVersionUID = 6110525555358536529L;
	
	private final String name, type;
	
	public Column(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "Column [name=" + name + ", type=" + type + "]";
	}
	
}
