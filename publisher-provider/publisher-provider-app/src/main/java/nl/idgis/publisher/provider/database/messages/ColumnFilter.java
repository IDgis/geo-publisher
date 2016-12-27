package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;
import java.util.Objects;

public class ColumnFilter implements Filter, Serializable {

	private static final long serialVersionUID = 8774684539028786369L;

	private final DatabaseColumnInfo column;
	
	private final String operator;
	
	private final Object value;
	
	public ColumnFilter(DatabaseColumnInfo column, String operator, Object value) {
		this.column = Objects.requireNonNull(column, "column should not be null");
		this.operator = Objects.requireNonNull(operator, "operator should not be null");
		this.value = Objects.requireNonNull(value, "value should not be null");
	}

	public DatabaseColumnInfo getColumn() {
		return column;
	}	
	
	public String getOperator() {
		return operator;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "SimpleColumnFilter [column=" + column + ", value=" + value + "]";
	}
}
