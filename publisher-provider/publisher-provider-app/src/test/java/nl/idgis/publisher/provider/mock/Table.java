package nl.idgis.publisher.provider.mock;

import java.util.List;

import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.TableInfo;

public class Table {
	
	private final TableInfo tableInfo;
	
	private final List<Record> records;
	
	public Table(TableInfo tableInfo, List<Record> records) {
		this.tableInfo = tableInfo;
		this.records = records;
	}

	public TableInfo getTableInfo() {
		return tableInfo;
	}

	public List<Record> getRecords() {
		return records;
	}
}