package nl.idgis.publisher.provider.protocol;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.stream.messages.Item;

/**
 * Multiple {@link Record} objects packed into a single object.
 * 
 * @author copierrj
 *
 */
public class Records extends Item {
	
	private static final long serialVersionUID = -1054052332134834469L;
	
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
