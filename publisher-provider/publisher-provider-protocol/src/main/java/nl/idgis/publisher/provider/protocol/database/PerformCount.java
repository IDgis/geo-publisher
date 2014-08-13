package nl.idgis.publisher.provider.protocol.database;

import java.io.Serializable;

public class PerformCount implements Serializable {
	
	private static final long serialVersionUID = -4222784285589898795L;
	
	private final String tableName;
	
	public PerformCount(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "PerformCount [tableName=" + tableName + "]";
	}
}
