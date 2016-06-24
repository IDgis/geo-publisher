package nl.idgis.publisher.provider.database.messages;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.provider.protocol.ColumnInfo;

public class FetchTable extends StreamingQuery {		

	private static final long serialVersionUID = -2891224433843529687L;
	
	private final String tableName;
	private final List<ColumnInfo> columnInfos;
	private final int messageSize;
	
	public FetchTable(String tableName, List<ColumnInfo> columnInfos, int messageSize) {
		this.tableName = tableName;
		this.columnInfos = columnInfos;
		this.messageSize = messageSize;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<ColumnInfo> getColumnInfos() {
		return Collections.unmodifiableList(columnInfos);
	}
	
	public int getMessageSize() {
		return messageSize;
	}

	@Override
	public String toString() {
		return "FetchTable [tableName=" + tableName + ", columnInfos="
				+ columnInfos + ", messageSize=" + messageSize + "]";
	}
	
}
