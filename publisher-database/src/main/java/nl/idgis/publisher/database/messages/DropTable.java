package nl.idgis.publisher.database.messages;

public class DropTable extends Query {	

	private static final long serialVersionUID = 330452387004908151L;
	
	private final String schemaName, tableName;
	
	public DropTable(String schemaName, String tableName) {
		this.schemaName = schemaName;
		this.tableName = tableName;		
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "DropTable [schemaName=" + schemaName + ", tableName="
				+ tableName + "]";
	}
	
}
