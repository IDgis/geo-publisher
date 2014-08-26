package nl.idgis.publisher.loader;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.web.Filter.ColumnReferenceExpression;
import nl.idgis.publisher.domain.web.Filter.FilterExpression;
import nl.idgis.publisher.domain.web.Filter.OperatorExpression;
import nl.idgis.publisher.domain.web.Filter.OperatorType;
import nl.idgis.publisher.domain.web.Filter.ValueExpression;
import nl.idgis.publisher.provider.protocol.database.Record;

public class FilterEvaluator {
	
	private abstract static class Value<T> {
		
		T t;
		
		Value(T t) {
			this.t = t;
		}
		
		T getValue() {
			return t;
		}
		
		abstract Type getType();
		
		static Value<?> toValue(Type type, Object value) {
			switch(type) {
				case BOOLEAN:
					return new BooleanValue((Boolean)value);
				case NUMERIC:					
					return new NumericValue(new BigDecimal(((Number)value).doubleValue()));
				case DATE:
				
				case GEOMETRY:				
				
				case TEXT:
				
				default:
					throw new IllegalArgumentException("unknown type: " + type);			
			}
		}
		
		static Value<?> toValue(Type type, String value) {
			switch(type) {
				case BOOLEAN:
					return new BooleanValue(Boolean.parseBoolean(value));
				case NUMERIC:
					return new NumericValue(new BigDecimal(value));								
				case DATE:
					
				case GEOMETRY:			
				
				case TEXT:
				
				default:
					throw new IllegalArgumentException("unknown type: " + type);			
			}
		}

		@Override
		public String toString() {
			return "Value [value=" + t + ", type=" + getType() + "]";
		}
	}
	
	private static class BooleanValue extends Value<Boolean> {
		
		public static BooleanValue TRUE = new BooleanValue(true);
		public static BooleanValue FALSE = new BooleanValue(false);
		
		BooleanValue(Boolean b) {
			super(b);
		}
		
		@Override
		Type getType() {
			return Type.BOOLEAN;
		}		
	}
	
	private static class NumericValue extends Value<BigDecimal> {

		NumericValue(BigDecimal bd) {
			super(bd);
		}

		@Override
		Type getType() {
			return Type.NUMERIC;
		}
		
	}
	
	private final Map<Column, Integer> columns;
	private final FilterExpression rootExpression;
	
	public FilterEvaluator(List<Column> columns, FilterExpression rootExpression) {		
		this.rootExpression = rootExpression;
		
		this.columns = new HashMap<>();
		
		int i = 0;
		for(Column column : columns) {
			this.columns.put(column, i++);
		}
	}

	protected Value<?> evaluate(Record record, FilterExpression expression) {
		if(expression instanceof ColumnReferenceExpression) {
			return evaluate(record, (ColumnReferenceExpression)expression);
		} else if(expression instanceof OperatorExpression) {
			return evaluate(record, (OperatorExpression)expression);
		} else if(expression instanceof ValueExpression) {
			return evaluate(record, (ValueExpression)expression);
		} else {
			throw new IllegalArgumentException("unknown expression type: " + expression.getClass());
		}
	}
	
	protected Value<?> evaluate(Record record, ValueExpression value) {
		Type type = value.getValueType();
		String s = value.getValue();
		
		return Value.toValue(type, s);
	}
	
	protected Value<?> evaluate(Record record, OperatorExpression operator) {
		OperatorType type = operator.getOperatorType();
		List<FilterExpression> inputs = operator.getInputs();
		
		switch(type) {
			case OR:
				for(FilterExpression expression : inputs) {
					Object expressionResult = evaluate(record, expression);
					if(expressionResult instanceof BooleanValue) {
						if(((BooleanValue) expressionResult).getValue()) {
							return BooleanValue.TRUE;
						}
					} else {
						throw new IllegalStateException("boolean input expected");
					}
				}
				
				return BooleanValue.FALSE;
				
			case AND:
				for(FilterExpression expression : inputs) {
					Object expressionResult = evaluate(record, expression);
					if(expressionResult instanceof BooleanValue) {
						if(!((BooleanValue) expressionResult).getValue()) {
							return BooleanValue.FALSE;
						}
					} else {
						throw new IllegalStateException("boolean input expected");
					}
				}
				
				return BooleanValue.TRUE;
				
			case LESS_THAN_EQUAL:
				if(compareNumeric(record, inputs) <= 0) {
					return BooleanValue.FALSE;
				}
				
				return BooleanValue.TRUE;
				
			default:
				throw new IllegalArgumentException("unknown operator type: " + type);
		}
	}

	protected int compareNumeric(Record record, List<FilterExpression> inputs) {
		if(inputs.size() != 2) {
			throw new IllegalArgumentException("exactly 2 inputs expected");
		}
						
		Value<?> input0 = evaluate(record, inputs.get(0));
		Value<?> input1 = evaluate(record, inputs.get(1));
		
		if(input0.getType() == Type.NUMERIC) {
			if(input1.getType() == Type.NUMERIC) {
				NumericValue numeric0 = (NumericValue)input0;
				NumericValue numeric1 = (NumericValue)input1;
				
				return numeric0.getValue().compareTo(numeric1.getValue());
			} else {
				throw new IllegalStateException("input1 is not a numeric:" + input1);
			}
		} else {
			throw new IllegalStateException("input0 is not a numeric: " + input0);
		}
	}
	
	protected Value<?> evaluate(Record record, ColumnReferenceExpression columnReference) {
		List<Object> values = record.getValues();
		
		Column column = columnReference.getColumn();
		Integer index = columns.get(column);
		if(index == null) {
			throw new IllegalArgumentException("unknown column");
		}
		
		return Value.toValue(column.getDataType(), values.get(index));
	}

	public boolean evaluate(Record record) {
		Object result = evaluate(record, rootExpression);
		
		if(result instanceof BooleanValue) {
			return ((BooleanValue) result).getValue();
		} else {
			throw new IllegalStateException("filter didn't evaluate to boolean");
		}
	}
}
