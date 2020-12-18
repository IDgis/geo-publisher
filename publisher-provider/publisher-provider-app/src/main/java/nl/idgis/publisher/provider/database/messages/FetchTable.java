package nl.idgis.publisher.provider.database.messages;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import nl.idgis.publisher.database.messages.StreamingQuery;

public class FetchTable extends StreamingQuery {

	private static final long serialVersionUID = 589029130785645339L;
	
	private final String tableName;
	
	private final List<AbstractDatabaseColumnInfo> columns;
	
	private final int messageSize;
	
	private final Filter filter;
	
	public FetchTable(String tableName, List<AbstractDatabaseColumnInfo> columns, int messageSize) {
		this(tableName, columns, messageSize, null);
	}
	
	public FetchTable(String tableName, List<AbstractDatabaseColumnInfo> columns, int messageSize, Filter filter) {
		this.tableName = tableName;
		this.columns = columns;
		this.messageSize = messageSize;
		this.filter = filter;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public List<AbstractDatabaseColumnInfo> getColumns() {
		return Collections.unmodifiableList(columns);
	}
	
	public int getMessageSize() {
		return messageSize;
	}
	
	public Optional<Filter> getFilter() {
		return Optional.ofNullable(filter);
	}

	@Override
	public String toString() {
		return "FetchTable [tableName=" + tableName + ", columns=" + columns + ", messageSize=" + messageSize
				+ ", filter=" + filter + "]";
	}
	
}
