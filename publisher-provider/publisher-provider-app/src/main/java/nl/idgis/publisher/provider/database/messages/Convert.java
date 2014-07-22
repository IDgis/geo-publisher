package nl.idgis.publisher.provider.database.messages;

public class Convert {
	
	private final Object value;

	public Convert(Object value) {		
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "Convert [value=" + value + "]";
	}
}
