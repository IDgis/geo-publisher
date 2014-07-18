package nl.idgis.publisher.protocol.database;

import java.io.Serializable;

public class Column implements Serializable {
	
	private static final long serialVersionUID = -5278210987617964061L;
	
	private final String name;
	private final String type;
	
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
