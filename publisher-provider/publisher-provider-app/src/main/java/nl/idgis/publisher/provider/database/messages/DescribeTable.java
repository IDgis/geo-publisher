package nl.idgis.publisher.provider.database.messages;

import nl.idgis.publisher.database.messages.Query;

public class DescribeTable extends Query {

	private static final long serialVersionUID = -7243209795589257031L;
	
	private final String tableName;
	
	public DescribeTable(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "DescribeTable [tableName=" + tableName + "]";
	}
}
