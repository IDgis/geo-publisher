package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class OnPut implements Serializable {		

	private static final long serialVersionUID = 1170969781722435976L;
	
	private final Class<?> entity;
	
	public OnPut(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "OnPut [entity=" + entity + "]";
	}
}
