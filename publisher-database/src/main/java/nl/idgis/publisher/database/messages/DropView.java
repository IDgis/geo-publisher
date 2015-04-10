package nl.idgis.publisher.database.messages;

public class DropView extends Query {	

	private static final long serialVersionUID = 6273434856962771787L;
	
	private final String schemaName, viewName;
	
	public DropView(String schemaName, String viewName) {
		this.schemaName = schemaName;
		this.viewName = viewName;		
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getViewName() {
		return viewName;
	}

	@Override
	public String toString() {
		return "DropView [schemaName=" + schemaName + ", viewName="
				+ viewName + "]";
	}
	
}
