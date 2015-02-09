package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class AddQuery implements Serializable {	

	private static final long serialVersionUID = -6530132079755949454L;
	
	private final Class<?> clazz;
	
	public AddQuery(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public Class<?> getClazz() {
		return clazz;
	}

	@Override
	public String toString() {
		return "AddQuery [clazz=" + clazz + "]";
	}
}
