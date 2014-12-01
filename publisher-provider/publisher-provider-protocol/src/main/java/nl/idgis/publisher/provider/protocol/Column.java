package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

import nl.idgis.publisher.domain.service.Type;

public class Column implements Serializable {
	
	private static final long serialVersionUID = -5278210987617964061L;
	
	private final String name;
	private final Type type;
	
	public Column(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return "Column [name=" + name + ", type=" + type + "]";
	}
}
