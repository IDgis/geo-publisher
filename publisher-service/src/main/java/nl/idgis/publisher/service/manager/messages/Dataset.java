package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class Dataset implements Serializable {

	private static final long serialVersionUID = -8354428462371875977L;
	
	private final String id, schemaName, tableName;
	
	public Dataset(String id, String schemaName, String tableName) {
		this.id = id;
		this.schemaName = schemaName;
		this.tableName = tableName;
	}
	
	public String getId() {
		return id;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "Dataset [id=" + id + ", schemaName=" + schemaName
				+ ", tableName=" + tableName + "]";
	}
}
