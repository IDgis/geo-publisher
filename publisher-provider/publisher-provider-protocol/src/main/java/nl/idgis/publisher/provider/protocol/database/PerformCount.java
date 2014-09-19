package nl.idgis.publisher.provider.protocol.database;

import nl.idgis.publisher.database.messages.Query;

public class PerformCount extends Query {		

	private static final long serialVersionUID = 8499023090069062665L;
	
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
