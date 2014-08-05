package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class InsertRecord extends Query {

	private static final long serialVersionUID = 7172706083489765137L;
	
	private final String tableName;
	private final List<Column> columns;
	private final List<Object> values;
	
	public InsertRecord(String tableName, List<Column> columns, List<Object> values) {
		this.tableName = tableName;
		this.columns = columns;
		this.values = values;
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
		return "InsertRecord [tableName=" + tableName + ", columns=" + columns
				+ ", values=" + values + "]";
	}

	
}
