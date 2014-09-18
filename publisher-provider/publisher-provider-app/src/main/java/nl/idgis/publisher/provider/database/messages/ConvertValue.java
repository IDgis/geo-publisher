package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

public class ConvertValue implements Serializable {

	private static final long serialVersionUID = -6889915789134347280L;
	
	private final Object value;

	public ConvertValue(Object value) {		
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "ConvertValue [value=" + value + "]";
	}
}
