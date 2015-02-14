package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class DoList implements Serializable {	

	private static final long serialVersionUID = 8259674619671756250L;
	
	private final Class<?> entity;
	
	public DoList(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "DoList [entity=" + entity + "]";
	}
}
