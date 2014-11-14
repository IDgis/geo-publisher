package nl.idgis.publisher.service.messages;

import java.io.Serializable;

public class Layer implements Serializable {

	private static final long serialVersionUID = -572694801554952735L;
	
	private final String name, schemaName, tableName;
	
	public Layer(String name, String schemaName, String tableName) {
		this.name = name;
		this.schemaName = schemaName;
		this.tableName = tableName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getSchemaName() {
		return schemaName;
	}
	
	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "Layer [name=" + name + ", schemaName=" + schemaName
				+ ", tableName=" + tableName + "]";
	}
}
