package nl.idgis.publisher.provider.protocol.database;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.database.messages.Query;

public class FetchTable extends Query {	

	private static final long serialVersionUID = -4715978918212247943L;
	
	private final String tableName;
	private final List<String> columnNames;
	private final int messageSize;
	
	public FetchTable(String tableName, List<String> columnNames, int messageSize) {
		this.tableName = tableName;
		this.columnNames = columnNames;
		this.messageSize = messageSize;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<String> getColumnNames() {
		return Collections.unmodifiableList(columnNames);
	}
	
	public int getMessageSize() {
		return messageSize;
	}

	@Override
	public String toString() {
		return "FetchTable [tableName=" + tableName + ", columnNames="
				+ columnNames + ", messageSize=" + messageSize + "]";
	}
	
}
