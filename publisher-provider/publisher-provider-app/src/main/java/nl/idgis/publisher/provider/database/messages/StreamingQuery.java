package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.stream.messages.Start;

public class StreamingQuery extends Start implements Serializable {
	
	private static final long serialVersionUID = 3463657688909957892L;
	
	private final String sql;
	private final int messageSize;
	
	public StreamingQuery(String sql, int messageSize) {
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
		return "StreamingQuery [sql=" + sql + ", messageSize=" + messageSize + "]";
	}
	
}
