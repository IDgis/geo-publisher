package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class CreateIndices extends Query {	

	private static final long serialVersionUID = 6556806951259239796L;

	private final String schemaName, tableName;
	
	private final List<Column> columns;
	
	public CreateIndices(String schemaName, String tableName, List<Column> columns) {	
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
		return columns;
	}

	@Override
	public String toString() {
		return "CreateIndices [schemaName=" + schemaName + ", tableName="
				+ tableName + ", columns=" + columns + "]";
	}
}
