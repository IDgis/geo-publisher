package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class OnDelete implements Serializable {	

	private static final long serialVersionUID = 8818230772651796892L;
	
	private final Class<?> entity;
	
	public OnDelete(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "OnDelete [entity=" + entity + "]";
	}
}
