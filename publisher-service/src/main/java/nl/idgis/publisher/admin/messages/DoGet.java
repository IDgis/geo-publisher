package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class DoGet implements Serializable {

	private static final long serialVersionUID = 8222097118783494481L;
	
	private final Class<?> entity;
	
	public DoGet(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "DoGet [entity=" + entity + "]";
	}
}
