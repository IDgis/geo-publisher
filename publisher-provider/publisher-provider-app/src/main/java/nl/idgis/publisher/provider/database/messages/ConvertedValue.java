package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

public class ConvertedValue implements Serializable {	
	
	private static final long serialVersionUID = -165753180878067072L;
	
	private final Object value;

	public ConvertedValue(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return "ConvertedValue [value=" + value + "]";
	}
}
