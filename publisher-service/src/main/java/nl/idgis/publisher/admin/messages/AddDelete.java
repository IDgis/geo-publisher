package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class AddDelete implements Serializable {	

	private static final long serialVersionUID = -3734801063309425239L;
	
	private final Class<?> entity;
	
	public AddDelete(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "AddDelete [entity=" + entity + "]";
	}
}
