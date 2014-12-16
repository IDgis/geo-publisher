package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * A single vector dataset record.
 * 
 * @author copierrj
 *
 */
public class Record implements Serializable {
		
	private static final long serialVersionUID = 4749264221713684724L;
	
	private final List<Object> values;
	
	/**
	 * Creates a vector dataset record.
	 * @param values a list of values.
	 */
	public Record(List<Object> values) {
		this.values = values;
	}
	
	/**
	 * 
	 * @return the values of this record
	 */
	public List<Object> getValues() {
		return Collections.unmodifiableList(this.values);
	}

	@Override
	public String toString() {
		return "Record [values=" + values + "]";
	}
	
}
