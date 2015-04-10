package nl.idgis.publisher.database.messages;

public class CreateView extends Query {

	private static final long serialVersionUID = -3304592267287723839L;
	
	private final String schemaName, viewName, sourceSchemaName, sourceTableName;
	
	public CreateView(String schemaName, String viewName, String sourceSchemaName, String sourceTableName) {
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
		return "CreateView [schemaName=" + schemaName + ", viewName="
				+ viewName + ", sourceSchemaName=" + sourceSchemaName
				+ ", sourceTableName=" + sourceTableName + "]";
	}
	
}
