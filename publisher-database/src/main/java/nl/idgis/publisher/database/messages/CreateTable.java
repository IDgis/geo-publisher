package nl.idgis.publisher.database.messages;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class CreateTable extends Query {
	
	private static final long serialVersionUID = 5649269363522788992L;
	
	private final String schemaName, tableName;
	private final List<Column> columns;
	
	public CreateTable(String schemaName, String tableName, List<Column> columns) {
		this.schemaName = schemaName;
		this.tableName = tableName;
		this.columns = columns;
	}
	
	public String getSchemaName() {
		return schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	public List<Column> getColumns() {
		return Collections.unmodifiableList(columns);
	}

	@Override
	public String toString() {
		return "CreateTable [schemaName=" + schemaName + ", tableName="
				+ tableName + ", columns=" + columns + "]";
	}
	
	
}
