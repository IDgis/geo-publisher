package nl.idgis.publisher.provider.database.messages;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import nl.idgis.publisher.database.messages.StreamingQuery;

public class FetchTable extends StreamingQuery {

	private static final long serialVersionUID = 8354005402803760927L;

	private final String scheme;

	private final String tableName;
	
	private final List<AbstractDatabaseColumnInfo> columns;
	
	private final int messageSize;
	
	private final Filter filter;
	
	public FetchTable(String tableName, List<AbstractDatabaseColumnInfo> columns, int messageSize) {
		this(tableName, columns, messageSize, null);
	}
	
	public FetchTable(String tableName, List<AbstractDatabaseColumnInfo> columns, int messageSize, Filter filter) {
		this.columns = columns;
		this.messageSize = messageSize;
		this.filter = filter;

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

	public String getScheme() { return scheme; }
	
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
		return "FetchTable{" +
				"scheme='" + scheme + '\'' +
				", tableName='" + tableName + '\'' +
				", columns=" + columns +
				", messageSize=" + messageSize +
				", filter=" + filter +
				'}';
	}

}
