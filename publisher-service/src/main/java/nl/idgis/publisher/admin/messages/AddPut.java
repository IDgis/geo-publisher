package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class AddPut implements Serializable {

	private static final long serialVersionUID = -5552564741339372575L;
	
	private final Class<?> entity;
	
	public AddPut(Class<?> entity) {
		this.entity = entity;
	}
	
	public Class<?> getEntity() {
		return entity;
	}

	@Override
	public String toString() {
		return "AddPut [entity=" + entity + "]";
	}
}
