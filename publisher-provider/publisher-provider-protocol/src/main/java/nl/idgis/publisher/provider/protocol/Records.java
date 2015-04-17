package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Multiple {@link Record} objects packed into a single object.
 * 
 * @author copierrj
 *
 */
public class Records implements Serializable {
	
	private static final long serialVersionUID = 8326324747896234038L;
	
	private final List<Record> records;
	
	/**
	 * Packs multiple {@link Record} objects into a single object.
	 * @param records
	 */
	public Records(List<Record> records) {
		this.records = records;
	}
	
	/**
	 * 
	 * @return containing records
	 */
	public List<Record> getRecords() {
		return Collections.unmodifiableList(records);
	}

	@Override
	public String toString() {
		return "Records [records=" + records + "]";
	}
}
