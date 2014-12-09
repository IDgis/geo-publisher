package nl.idgis.publisher.provider.mock.messages;

import java.io.Serializable;
import java.util.List;

import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.database.Record;

public class PutTable implements Serializable {

	private static final long serialVersionUID = -7637942156916989354L;

	private final String tableName;
	
	private final TableDescription tableDescription;
	private final List<Record> records;
	
	public PutTable(String tableName, TableDescription tableDescription, List<Record> records) {
		this.tableName = tableName;
		this.tableDescription = tableDescription;		
		this.records = records;
	}

	public String getTableName() {
		return tableName;
	}

	public TableDescription getTableDescription() {
		return tableDescription;
	}
	
	public List<Record> getRecords() {
		return records;
	}

	@Override
	public String toString() {
		return "PutTable [tableName=" + tableName + ", tableDescription="
				+ tableDescription + ", records=" + records + "]";
	}	
}
