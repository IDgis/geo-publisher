package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class InsertRecords extends Query {
	
	private static final long serialVersionUID = -4918284219202902423L;

	private final String schemaName, tableName;
	
	private final List<Column> columns;
	
	private final List<List<Object>> records;
	
	public InsertRecords(String schemaName, String tableName, List<Column> columns, List<List<Object>> records) {
		this.schemaName = schemaName;
		this.tableName = tableName;
		this.columns = columns;
		this.records = records;
	}
	
	public String getSchemaName() {
		return schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public List<List<Object>> getRecords() {
		return records;
	}

	@Override
	public String toString() {
		return "InsertRecords [schemaName=" + schemaName + ", tableName="
				+ tableName + ", columns=" + columns + ", records=" + records
				+ "]";
	}

	

	
}
