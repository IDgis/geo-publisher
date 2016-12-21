package nl.idgis.publisher.provider.mock.messages;

import java.io.Serializable;
import java.util.List;

import nl.idgis.publisher.provider.database.messages.DatabaseTableInfo;
import nl.idgis.publisher.provider.protocol.Record;

public class PutTable implements Serializable {

	private static final long serialVersionUID = -4614221763403565438L;

	private final String tableName;
	
	private final DatabaseTableInfo tableInfo;
	private final List<Record> records;
	
	public PutTable(String tableName, DatabaseTableInfo tableInfo, List<Record> records) {
		this.tableName = tableName;
		this.tableInfo = tableInfo;		
		this.records = records;
	}

	public String getTableName() {
		return tableName;
	}

	public DatabaseTableInfo getTableInfo() {
		return tableInfo;
	}
	
	public List<Record> getRecords() {
		return records;
	}

	@Override
	public String toString() {
		return "PutTable [tableName=" + tableName + ", tableInfo="
				+ tableInfo + ", records=" + records + "]";
	}	
}
