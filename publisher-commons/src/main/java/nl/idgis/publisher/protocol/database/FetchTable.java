package nl.idgis.publisher.protocol.database;

import java.io.Serializable;

public class FetchTable implements Serializable {
	
	private static final long serialVersionUID = 3870743003841842250L;
	
	private final String tableName;
	
	public FetchTable(String tableName) {
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return tableName;
	}
}
