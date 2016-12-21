package nl.idgis.publisher.provider.database.messages;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.database.messages.StreamingQuery;

public class FetchTable extends StreamingQuery {		

	private static final long serialVersionUID = -2891224433843529687L;
	
	private final String tableName;
	
	private final List<DatabaseColumnInfo> columns;
	
	private final int messageSize;
	
	public FetchTable(String tableName, List<DatabaseColumnInfo> columns, int messageSize) {
		this.tableName = tableName;
		this.columns = columns;
		this.messageSize = messageSize;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<DatabaseColumnInfo> getColumns() {
		return Collections.unmodifiableList(columns);
	}
	
	public int getMessageSize() {
		return messageSize;
	}

	@Override
	public String toString() {
		return "FetchTable [tableName=" + tableName + ", columnInfos="
				+ columns + ", messageSize=" + messageSize + "]";
	}
	
}
