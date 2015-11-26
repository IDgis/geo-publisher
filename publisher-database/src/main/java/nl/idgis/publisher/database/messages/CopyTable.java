package nl.idgis.publisher.database.messages;

public class CopyTable extends Query {
	
	private static final long serialVersionUID = 3351471538454066209L;
	
	private final String schemaName, viewName, sourceSchemaName, sourceTableName;
	
	public CopyTable(String schemaName, String viewName, String sourceSchemaName, String sourceTableName) {
		this.schemaName = schemaName;
		this.viewName = viewName;
		this.sourceSchemaName = sourceSchemaName;
		this.sourceTableName = sourceTableName;
	}
	
	public String getSchemaName() {
		return schemaName;
	}

	public String getViewName() {
		return viewName;
	}

	public String getSourceSchemaName() {
		return sourceSchemaName;
	}

	public String getSourceTableName() {
		return sourceTableName;
	}

	@Override
	public String toString() {
		return "CopyTable [schemaName=" + schemaName + ", viewName="
				+ viewName + ", sourceSchemaName=" + sourceSchemaName
				+ ", sourceTableName=" + sourceTableName + "]";
	}
	
}
