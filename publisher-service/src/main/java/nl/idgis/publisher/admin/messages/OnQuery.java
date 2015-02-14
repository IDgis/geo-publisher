package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class OnQuery implements Serializable {

	private static final long serialVersionUID = 857619253569573006L;
	
	private final Class<?> clazz;
	
	public OnQuery(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public Class<?> getClazz() {
		return clazz;
	}

	@Override
	public String toString() {
		return "OnQuery [clazz=" + clazz + "]";
	}
}
