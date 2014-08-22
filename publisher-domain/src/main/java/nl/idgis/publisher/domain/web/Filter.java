package nl.idgis.publisher.domain.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonInclude (Include.NON_NULL)
public final class Filter extends Entity {

	private static final long serialVersionUID = -5558664873558620528L;
	
	private final FilterExpression expression;
	
	@JsonCreator
	public Filter (final @JsonProperty(value = "expression") FilterExpression expression) {
		this.expression = expression;
	}
	
	public FilterExpression getExpression () {
		return expression;
	}

	public static enum OperatorType {
		AND (0),
		OR (0),
		
		EQUALS (2),
		NOT_EQUALS (2),
		LESS_THAN (2),
		LESS_THAN_EQUAL (2),
		GREATER_THAN (2),
		GREATER_THAN_EQUAL (2),
		
		LIKE (2),
		
		IN (2),
		
		NOT_NULL (1);
		
		private final int arity;
		
		OperatorType (int arity) {
			this.arity = arity;
		}
		
		public int getArity () {
			return arity;
		}
	}

	@JsonTypeInfo (
		use = Id.NAME,
		include = As.PROPERTY,
		property = "type"
	)
	@JsonSubTypes ({
		@JsonSubTypes.Type (name = "column-ref", value = ColumnReferenceExpression.class),
		@JsonSubTypes.Type (name = "operator", value = OperatorExpression.class),
		@JsonSubTypes.Type (name = "value", value = ValueExpression.class)
	})
	public abstract static class FilterExpression extends Entity {
		private static final long serialVersionUID = 7436268482215073429L;
	}
	
	public final static class ColumnReferenceExpression extends FilterExpression {
		private static final long serialVersionUID = -6056879874863089428L;
		
		private final Column column;
		
		@JsonCreator
		public ColumnReferenceExpression (final @JsonProperty("column") Column column) {
			if (column == null) {
				throw new NullPointerException ("column cannot be null");
			}
			
			this.column = column;
		}
		
		public Column getColumn () {
			return column;
		}
	}
	
	@JsonInclude (Include.NON_EMPTY)
	public final static class OperatorExpression extends FilterExpression {
		private static final long serialVersionUID = 7557590341405024974L;
		
		private final OperatorType operatorType;
		private final List<FilterExpression> inputs;
		
		@JsonCreator
		public OperatorExpression (
				final @JsonProperty("operatorType") OperatorType operatorType, 
				final @JsonProperty("inputs") List<FilterExpression> inputs) {
			
			if (operatorType == null) {
				throw new NullPointerException ("operatorType cannot be null");
			}
			
			this.operatorType = operatorType;
			this.inputs = inputs == null ? Collections.<FilterExpression>emptyList () : new ArrayList<Filter.FilterExpression> (inputs);
			
			// Test arity:
			if (operatorType.getArity () > 0 && this.inputs.size () != operatorType.getArity ()) {
				throw new IllegalArgumentException ("Invalid number of inputs " + this.inputs.size () + ", expected " + operatorType.getArity ());
			}
		}

		public OperatorType getOperatorType() {
			return operatorType;
		}

		public List<FilterExpression> getInputs() {
			return Collections.unmodifiableList (inputs);
		}
	}
	
	public final static class ValueExpression extends FilterExpression {
		private static final long serialVersionUID = 7253420300019820775L;
		
		private final Type valueType;
		private final String value;
		
		@JsonCreator
		public ValueExpression (
				final @JsonProperty ("type") Type valueType,
				final @JsonProperty ("valueType") String value) {
			
			if (valueType == null) {
				throw new NullPointerException ("valueType cannot be null");
			}
			if (value == null) {
				throw new NullPointerException ("value cannot be null");
			}
			
			this.valueType = valueType;
			this.value = value;
		}

		public Type getValueType () {
			return valueType;
		}

		public String getValue () {
			return value;
		}
	}
}
