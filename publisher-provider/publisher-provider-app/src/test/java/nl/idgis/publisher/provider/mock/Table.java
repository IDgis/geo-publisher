package nl.idgis.publisher.provider.mock;

import java.util.List;

import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.protocol.Record;

public class Table {
	
	private final DatabaseTableInfo tableInfo;
	
	private final List<Record> records;
	
	public Table(DatabaseTableInfo tableInfo, List<Record> records) {
		this.tableInfo = tableInfo;
		this.records = records;
	}

	public DatabaseTableInfo getTableInfo() {
		return tableInfo;
	}

	public List<Record> getRecords() {
		return records;
	}
}