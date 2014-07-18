package nl.idgis.publisher.provider.protocol.database;

import java.io.Serializable;

public class DescribeTable implements Serializable {
	
	private static final long serialVersionUID = -5970461010970719731L;
	
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
