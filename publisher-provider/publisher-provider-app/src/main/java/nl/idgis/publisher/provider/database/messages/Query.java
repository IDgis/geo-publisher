package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.stream.messages.Start;

public class Query extends Start implements Serializable {
	
	private static final long serialVersionUID = 7073260568794388261L;
	
	private final String sql;
	private final int messageSize;
	
	public Query(String sql) {
		this(sql, 0);
	}
	
	public Query(String sql, int messageSize) {
		this.sql = sql;
		this.messageSize = messageSize;
	}

	public String getSql() {
		return sql;
	}
	
	public int getMessageSize() {
		return messageSize;
	}

	@Override
	public String toString() {
		return "Query [sql=" + sql + ", messageSize=" + messageSize + "]";
	}
	
}
