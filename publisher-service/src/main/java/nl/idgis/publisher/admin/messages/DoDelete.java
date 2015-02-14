package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class DoDelete implements Serializable {

	private static final long serialVersionUID = -1930849765624002557L;
	
	private final Class<?> entity;
	
	public DoDelete(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "DoDelete [entity=" + entity + "]";
	}
}
