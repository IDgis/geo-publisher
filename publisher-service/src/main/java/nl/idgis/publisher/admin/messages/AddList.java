package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class AddList implements Serializable {

	private static final long serialVersionUID = -4685807676984938315L;
	
	private final Class<?> entity;
	
	public AddList(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "AddList [entity=" + entity + "]";
	}
}
