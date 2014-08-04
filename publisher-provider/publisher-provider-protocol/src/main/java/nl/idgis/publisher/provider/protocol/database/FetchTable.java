package nl.idgis.publisher.provider.protocol.database;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.stream.messages.Start;

public class FetchTable extends Start implements Serializable {

	private static final long serialVersionUID = -7124841931666364023L;
	
	private final String tableName;
	private final List<String> columnNames;
	
	public FetchTable(String tableName, List<String> columnNames) {
		this.tableName = tableName;
		this.columnNames = columnNames;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(columnNames);
	}

	@Override
	public String toString() {
		return "FetchTable [tableName=" + tableName + ", columnNames="
				+ columnNames + "]";
	}
	
}
