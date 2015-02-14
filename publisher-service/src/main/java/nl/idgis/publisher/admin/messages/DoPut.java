package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class DoPut implements Serializable {
	
	private static final long serialVersionUID = -5231777614287190404L;
	
	private final Class<?> entity;
	
	public DoPut(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "DoPut [entity=" + entity + "]";
	}
}
