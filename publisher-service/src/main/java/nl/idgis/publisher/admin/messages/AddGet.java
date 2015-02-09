package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class AddGet implements Serializable {

	private static final long serialVersionUID = 2942126419913747436L;
	
	private final Class<?> entity;
	
	public AddGet(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "AddGet [entity=" + entity + "]";
	}
}
