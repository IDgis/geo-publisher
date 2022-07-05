package nl.idgis.publisher.loader;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.sql.Timestamp;

import akka.dispatch.Mapper;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.web.Filter.ColumnReferenceExpression;
import nl.idgis.publisher.domain.web.Filter.FilterExpression;
import nl.idgis.publisher.domain.web.Filter.OperatorExpression;
import nl.idgis.publisher.domain.web.Filter.OperatorType;
import nl.idgis.publisher.domain.web.Filter.ValueExpression;

import nl.idgis.publisher.provider.protocol.WKBGeometry;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.utils.SimpleDateFormatMapper;

public class FilterEvaluator {
	
	protected abstract static class Value<T> {
		
		static final Mapper<String, Date> STRING_DATE_MAPPER = SimpleDateFormatMapper.isoDateAndDateTime();
		
		private T t;
		
		Value(T t) {
			this.t = t;
		}
		
		T getValue() {
			return t;
		}
		
		boolean isComparable() {
			return false;
		}
		
		String getStringValue() {
			return t.toString().trim();
		}
		
		abstract Type getType();
		
		static Value<?> toValue(Type type, Object value) {
			if(value == null) {
				return new NullValue();
			}
			
			switch(type) {
				case BOOLEAN:
					return new BooleanValue((Boolean)value);
				case NUMERIC:					
					return new NumericValue(new BigDecimal(((Number)value).doubleValue()));
				case DATE:
				case TIMESTAMP:
					return new DateValue((Date)value);
				case GEOMETRY:
					return new GeometryValue((WKBGeometry)value);
				case TEXT:
					return new StringValue(value.toString());
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
				case TEXT:
					return new StringValue(value);
				case DATE:
				case TIMESTAMP:
					return new DateValue(STRING_DATE_MAPPER.apply(value));
				case GEOMETRY:
					throw new IllegalArgumentException("cannot convert string to geometry");
				default:
					throw new IllegalArgumentException("unknown type: " + type);			
			}
		}

		@Override
		public String toString() {
			return "Value [value=" + t + ", type=" + getType() + "]";
		}
	}
	
	protected static abstract class ComparableValue<T extends Comparable<T>> extends Value<T> {

		ComparableValue(T t) {
			super(t);
		}
		
		boolean isComparable() {
			return true;
		}
		
	}
	
	protected static class BooleanValue extends Value<Boolean> {
		
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
	
	protected static class NumericValue extends ComparableValue<BigDecimal> {

		NumericValue(BigDecimal bd) {
			super(bd);
		}

		@Override
		Type getType() {
			return Type.NUMERIC;
		}
		
	}
	
	protected static class StringValue extends ComparableValue<String> {

		StringValue(String s) {
			super(s);			
		}

		@Override
		Type getType() {
			return Type.TEXT;
		}
		
	}
	
	protected static class DateValue extends ComparableValue<Date> {

		DateValue(Date d) {
			super(d);
		}

		@Override
		Type getType() {
			return Type.DATE;
		}
		
		public String getStringValue() {
			Date date = getValue();
			 
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			
			return 
				toString(calendar.get(Calendar.YEAR), 4) +
				toString(calendar.get(Calendar.MONTH) + 1, 2) +
				toString(calendar.get(Calendar.DAY_OF_MONTH), 2) +
				"T" +
				toString(calendar.get(Calendar.HOUR_OF_DAY), 2) +
				":" +
				toString(calendar.get(Calendar.MINUTE), 2) +
				":" +
				toString(calendar.get(Calendar.SECOND), 2);
		}
		
		private String toString(int value, int digits) {
			String strValue = "" + value;
			
			StringBuilder sb = new StringBuilder();
			for(int i = strValue.length(); i < digits; i++) {
				sb.append(0);
			}
			
			sb.append(strValue);
			
			return sb.toString();
		}
		
	}
	
	protected static class GeometryValue extends Value<WKBGeometry> {

		GeometryValue(WKBGeometry g) {
			super(g);
		}

		@Override
		Type getType() {
			return Type.GEOMETRY;
		}
		
	}
	
	protected static class NullValue extends Value<Object> {

		NullValue() {
			super(null);
		}

		@Override
		Type getType() {
			return null;
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
	
	public static Set<Column> getRequiredColumns(FilterExpression expression) {
		Set<Column> columns = new HashSet<>();
		collectRequiredColumns(expression, columns);
		return Collections.unmodifiableSet(columns);
	}
	
	protected static void collectRequiredColumns(FilterExpression expression, Set<Column> columns) {
		if(expression instanceof ColumnReferenceExpression) {
			columns.add(((ColumnReferenceExpression) expression).getColumn());
		} else if(expression instanceof OperatorExpression) {
			for(FilterExpression input : ((OperatorExpression) expression).getInputs()) {
				collectRequiredColumns(input, columns);
			}
		} else if(expression instanceof ValueExpression) {
			// nothing to do
		} else {
			throw new IllegalArgumentException("unknown expression type: " + expression.getClass());
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
		OperatorType operatorType = operator.getOperatorType();
		
		List<Value<?>> inputs = new ArrayList<>();
		for(FilterExpression inputExpression : operator.getInputs()) {
			 Value<?> inputValue = evaluate(record, inputExpression);
			 
			 if(inputValue instanceof NullValue) {
				 return BooleanValue.FALSE;
			 } else {
				 inputs.add(inputValue);
			 }
		}
		
		int operatorArity = operatorType.getArity();
		if(operatorArity > 0 && inputs.size() != operatorArity) {
			throw new IllegalStateException("expected " + operatorArity + " inputs: " + inputs.size());
		}
		
		switch(operatorType) {
			case OR:
				return evaluateOr(record, inputs);				
			case AND:
				return evaluateAnd(record, inputs);				
			case EQUALS:
				return evaluateEquals(record, inputs);				
			case NOT_EQUALS:
				return evaluateNotEquals(record, inputs);				
			case LESS_THAN:
				return evaluateLessThan(record, inputs);				
			case LESS_THAN_EQUAL:
				return evaluateLessThanEqual(record, inputs);				
			case GREATER_THAN:
				return evaluateGreaterThan(record, inputs);				
			case GREATER_THAN_EQUAL:				
				return evaluateGreaterThanEqual(record, inputs);				
			case NOT_NULL:
				return evaluateNotNull(record, inputs);				
			case LIKE:			
				return evaluateLike(record, inputs);
			case IN:
				return evaluateIn(record, inputs);
				
			default:
				throw new IllegalArgumentException("unknown operator type: " + operatorType);
		}
	}

	private BooleanValue evaluateIn(Record record, List<Value<?>> inputs) {
		Value<?> value = inputs.get(0);
		
		String expr = inputs.get(1).getStringValue();
		for(String s : expr.split(",")) {
			if(compare(record, value, Value.toValue(value.getType(), s.trim())) == 0) {
				return BooleanValue.TRUE;
			}
		}
		
		return BooleanValue.FALSE;
	}

	private Value<?> evaluateLike(Record record, List<Value<?>> inputs) {
		String value = inputs.get(0).getStringValue();
		
		Value<?> expr = inputs.get(1);
		if(expr.getType() != Type.TEXT) {
			throw new IllegalArgumentException("string expression expected");
		}
		
		if(value.matches(
			expr.getStringValue()
			    .replace(".", "\\.")
			    .replace("?", ".")
			    .replace("%", ".*"))) {
			
			return BooleanValue.TRUE;
		}
		
		return BooleanValue.FALSE;
	}

	private Value<?> evaluateNotNull(Record record, List<Value<?>> inputs) {
		if(inputs.get(0) instanceof NullValue) {
			return BooleanValue.FALSE;
		} 
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateGreaterThanEqual(Record record, List<Value<?>> inputs) {
		if(compare(record, inputs) >= 0) {
			return BooleanValue.FALSE;
		}
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateGreaterThan(Record record, List<Value<?>> inputs) {
		if(compare(record, inputs) > 0) {
			return BooleanValue.FALSE;
		}
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateLessThanEqual(Record record, List<Value<?>> inputs) {
		if(compare(record, inputs) <= 0) {
			return BooleanValue.FALSE;
		}
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateLessThan(Record record, List<Value<?>> inputs) {
		if(compare(record, inputs) < 0) {
			return BooleanValue.FALSE;
		}
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateNotEquals(Record record, List<Value<?>> inputs) {
		if(compare(record, inputs) == 0) {
			return BooleanValue.FALSE;
		}
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateEquals(Record record, List<Value<?>> inputs) {
		if(compare(record, inputs) != 0) {
			return BooleanValue.FALSE;
		}
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateAnd(Record record, List<Value<?>> inputs) {
		for(Value<?> input : inputs) {
			if(input instanceof BooleanValue) {
				if(!((BooleanValue) input).getValue()) {
					return BooleanValue.FALSE;
				}
			} else {
				throw new IllegalStateException("boolean input expected");
			}
		}
		
		return BooleanValue.TRUE;
	}

	private Value<?> evaluateOr(Record record, List<Value<?>> inputs) {
		for(Value<?> input : inputs) {			
			if(input instanceof BooleanValue) {
				if(((BooleanValue) input).getValue()) {
					return BooleanValue.TRUE;
				}
			} else {
				throw new IllegalStateException("boolean input expected");
			}
		}
		
		return BooleanValue.FALSE;
	}

	private int compare(Record record, List<Value<?>> inputs) {
		Value<?> input0 = inputs.get(0);
		Value<?> input1 = inputs.get(1);
		
		return compare(record, input0, input1);
	}
		
	private int compare(Record record, Value<?> input0, Value<?> input1) {		
		if(input0.getType() != input1.getType()) {
			throw new IllegalStateException("inputs are of different types");
		}
		
		if(input0.isComparable()) {
			if(input1.isComparable()) {
				switch(input0.getType()) {
					case TEXT:
						StringValue string0 = (StringValue)input0;
						StringValue string1 = (StringValue)input1;
						
						return string0.getValue().compareTo(string1.getValue());
					case NUMERIC:
						NumericValue numeric0 = (NumericValue)input0;
						NumericValue numeric1 = (NumericValue)input1;
					
						return numeric0.getValue().compareTo(numeric1.getValue());					
					case DATE:
						DateValue date0 = (DateValue)input0;
						DateValue date1 = (DateValue)input1;
						
						return date0.getValue().compareTo(date1.getValue());
					default:
						throw new IllegalStateException("unknown comparable type: " + input0.getType());
				}
			} else {
				throw new IllegalStateException("input1 is not a comparable:" + input1);
			}
		} else {
			throw new IllegalStateException("input0 is not a comparable: " + input0);
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
