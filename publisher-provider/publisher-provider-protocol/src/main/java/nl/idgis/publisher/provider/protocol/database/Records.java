package nl.idgis.publisher.provider.protocol.database;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.stream.messages.Item;

public class Records extends Item {
	
	private static final long serialVersionUID = -1054052332134834469L;
	private final List<Record> records;
	
	public Records(List<Record> records) {
		this.records = records;
	}
	
	public List<Record> getRecords() {
		return Collections.unmodifiableList(records);
	}

	@Override
	public String toString() {
		return "Records [records=" + records + "]";
	}
}
