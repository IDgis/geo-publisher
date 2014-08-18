package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class InsertRecord extends Query {
	
	private static final long serialVersionUID = -6817649274369201580L;
	
	private final String schemaName, tableName;
	private final List<Column> columns;
	private final List<Object> values;
	
	public InsertRecord(String schemaName, String tableName, List<Column> columns, List<Object> values) {
		this.schemaName = schemaName;
		this.tableName = tableName;
		this.columns = columns;
		this.values = values;
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

	public List<Object> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return "InsertRecord [schemaName=" + schemaName + ", tableName="
				+ tableName + ", columns=" + columns + ", values=" + values
				+ "]";
	}

	

	
}
