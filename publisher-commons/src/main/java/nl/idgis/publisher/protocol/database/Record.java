package nl.idgis.publisher.protocol.database;

import java.io.Serializable;
import java.util.Arrays;

import nl.idgis.publisher.protocol.stream.Item;

public class Record extends Item implements Serializable {

	private static final long serialVersionUID = 144062896128532148L;
	
	private final Object[] values;
	
	public Record(Object[] values) {
		this.values = values;
	}
	
	public Object[] getValues() {
		return this.values;
	}

	@Override
	public String toString() {
		return "Record [values=" + Arrays.toString(values) + "]";
	}
}
