package nl.idgis.publisher.provider.database.messages;

import nl.idgis.publisher.database.messages.Query;

public class PerformCount extends Query {

	private static final long serialVersionUID = 1464334851511334051L;

	private final String scheme;
	
	private final String tableName;
	
	public PerformCount(String tableName) {

		String[] schemaTableParts = tableName.split("\\.");

		switch (schemaTableParts.length) {
			case 1: // only table
				this.scheme = null;
				this.tableName = schemaTableParts[0];
				break;
			case 2: // includes scheme in tablename
				this.scheme = schemaTableParts[0];
				this.tableName = schemaTableParts[1];
				break;
			default: // includes db and scheme in tablename
				this.scheme = schemaTableParts[1];
				this.tableName = schemaTableParts[2];
		}
	}

	public String getTableName() {
		return tableName;
	}

	public String getScheme() { return  scheme; }

	@Override
	public String toString() {
		return "PerformCount{" +
				"scheme='" + scheme + '\'' +
				", tableName='" + tableName + '\'' +
				'}';
	}
}
