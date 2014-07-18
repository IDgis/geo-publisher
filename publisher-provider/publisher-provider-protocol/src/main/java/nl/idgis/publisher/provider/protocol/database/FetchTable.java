package nl.idgis.publisher.provider.protocol.database;

import java.io.Serializable;

import nl.idgis.publisher.protocol.stream.Start;

public class FetchTable extends Start implements Serializable {
	
	private static final long serialVersionUID = 3870743003841842250L;
	
	private final String tableName;
	
	public FetchTable(String tableName) {
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return tableName;
	}

	@Override
	public String toString() {
		return "FetchTable [tableName=" + tableName + "]";
	}
}
