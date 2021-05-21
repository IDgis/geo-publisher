package nl.idgis.publisher.provider.database.messages;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public class ColumnFilter implements Filter, Serializable {

	private static final long serialVersionUID = 8774684539028786369L;

	private final AbstractDatabaseColumnInfo column;
	
	private final String operator;
	
	private final Object operand;
	
	public ColumnFilter(AbstractDatabaseColumnInfo column, String operator) {
		this(column, operator, null);
	}
	
	public ColumnFilter(AbstractDatabaseColumnInfo column, String operator, Object operand) {
		this.column = Objects.requireNonNull(column, "column should not be null");
		this.operator = Objects.requireNonNull(operator, "operator should not be null");
		this.operand = operand;
	}

	public AbstractDatabaseColumnInfo getColumn() {
		return column;
	}	
	
	public String getOperator() {
		return operator;
	}

	public Optional<Object> getOperand() {
		return Optional.ofNullable(operand);
	}

	@Override
	public String toString() {
		return "ColumnFilter [column=" + column + ", operator=" + operator + ", operand=" + operand + "]";
	}
	
}
