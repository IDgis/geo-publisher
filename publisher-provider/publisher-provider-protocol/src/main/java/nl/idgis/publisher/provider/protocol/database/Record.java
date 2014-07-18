package nl.idgis.publisher.provider.protocol.database;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.protocol.stream.Item;

public class Record extends Item implements Serializable {
	
	private static final long serialVersionUID = 5011984818623444511L;
	
	private final List<Object> values;
	
	public Record(List<Object> values) {
		this.values = values;
	}
	
	public List<Object> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	@Override
	public String toString() {
		return "Record [values=" + values + "]";
	}
	
}
