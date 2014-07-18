package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

public class Converted implements Serializable {	
	
	private static final long serialVersionUID = 5886839531054876794L;
	
	private final Object value;

	public Converted(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "Converted [value=" + value + "]";
	}
}
