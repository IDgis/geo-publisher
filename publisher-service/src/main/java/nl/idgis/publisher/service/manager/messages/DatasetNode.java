package nl.idgis.publisher.service.manager.messages;

import com.mysema.query.annotations.QueryProjection;

public class DatasetNode extends Node {

	private static final long serialVersionUID = 1125833789348571446L;
	
	private final String schemaName, tableName;
	
	@QueryProjection
	public DatasetNode(String id, String name, String title, String abstr, String schemaName, String tableName) {
		super(id, name, title, abstr);
		
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
		return "Dataset [schemaName=" + schemaName + ", tableName=" + tableName
				+ ", id=" + id + ", name=" + name + ", title=" + title
				+ ", abstr=" + abstr + "]";
	}	
}
