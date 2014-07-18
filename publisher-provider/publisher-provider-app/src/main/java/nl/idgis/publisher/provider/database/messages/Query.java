package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;

import nl.idgis.publisher.stream.messages.Start;

public class Query extends Start implements Serializable {
	
	private static final long serialVersionUID = -9096298412619198452L;
	
	private final String sql;
	
	public Query(String sql) {
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}

	@Override
	public String toString() {
		return "Query [sql=" + sql + "]";
	}
}
