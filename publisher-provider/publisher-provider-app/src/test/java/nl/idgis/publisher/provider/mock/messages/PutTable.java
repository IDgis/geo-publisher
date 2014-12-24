package nl.idgis.publisher.provider.mock.messages;

import java.io.Serializable;
import java.util.List;

import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.TableInfo;

public class PutTable implements Serializable {

	private static final long serialVersionUID = -1961423539738228564L;

	private final String tableName;
	
	private final TableInfo tableInfo;
	private final List<Record> records;
	
	public PutTable(String tableName, TableInfo tableInfo, List<Record> records) {
		this.tableName = tableName;
		this.tableInfo = tableInfo;		
		this.records = records;
	}

	public String getTableName() {
		return tableName;
	}

	public TableInfo getTableInfo() {
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
