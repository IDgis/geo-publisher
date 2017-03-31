package nl.idgis.publisher.domain.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
	
	private static class ColumnRef {
		
		private final String name;
		
		private final Type dataType;
		
		ColumnRef(String name, Type dataType) {
			this.name = name;
			this.dataType = dataType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ColumnRef other = (ColumnRef) obj;
			if (dataType != other.dataType)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		
	}

	public boolean isValid (final List<Column> columns) {
		final Set<ColumnRef> columnRefSet = columns.stream()
			.map(column -> new ColumnRef(column.getName(), column.getDataType()))
			.collect(Collectors.toSet());
		final LinkedList<Filter.FilterExpression> fringe = new LinkedList<> ();
		
		fringe.add (getExpression ());
		
		while (!fringe.isEmpty ()) {
			final Filter.FilterExpression expression = fringe.poll ();
			
			if (expression instanceof Filter.ColumnReferenceExpression) {
				Column column = ((Filter.ColumnReferenceExpression) expression).getColumn ();
				if (!columnRefSet.contains (new ColumnRef(column.getName(), column.getDataType()))) {
					return false;
				}
			} else if (expression instanceof Filter.OperatorExpression) {
				fringe.addAll (((Filter.OperatorExpression) expression).getInputs ());
			}
		}
		
		return true;
	}
	
	public static enum OperatorType {
		AND (0),
		OR (0),
		
		EQUALS (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		NOT_EQUALS (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		LESS_THAN (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		LESS_THAN_EQUAL (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		GREATER_THAN (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		GREATER_THAN_EQUAL (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		
		LIKE (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		
		IN (2, Type.DATE, Type.NUMERIC, Type.TEXT),
		
		NOT_NULL (1, Type.DATE, Type.NUMERIC, Type.TEXT, Type.GEOMETRY);
		
		private final int arity;
		private final Set<Type> supportedTypes;
		
		OperatorType (final int arity, final Type ... supportedTypes) {
			this.arity = arity;
			this.supportedTypes = new HashSet<> ();
			
			for (final Type supportedType: supportedTypes) {
				this.supportedTypes.add (supportedType);
			}
		}
		
		public int getArity () {
			return arity;
		}
		
		public Set<Type> getSupportedTypes () {
			return Collections.unmodifiableSet (supportedTypes);
		}
	}
	
	private final static Set<OperatorInput> validInputs = new HashSet<OperatorInput> () {
		private static final long serialVersionUID = 3876171589986580689L;

		{
			for (final OperatorType operatorType: OperatorType.values ()) {
				if (operatorType.getArity () == 0 || operatorType.equals (OperatorType.IN)) {
					continue;
				}
				
				for (final Type supportedType: operatorType.getSupportedTypes ()) {
					final Type[] types = new Type[operatorType.getArity ()];
					for (int i = 0; i < operatorType.getArity (); ++ i) {
						types[i] = supportedType;
					}
					add (new OperatorInput (operatorType, types));
				}
			}
			
			add (new OperatorInput (OperatorType.IN, Type.DATE, Type.TEXT));
			add (new OperatorInput (OperatorType.IN, Type.NUMERIC, Type.TEXT));
			add (new OperatorInput (OperatorType.IN, Type.TEXT, Type.TEXT));
		}
	};

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
			for (final FilterExpression input: inputs) {
				if (input == null) {
					throw new IllegalArgumentException ("Input cannot be null");
				}
			}
			
			// Test inputs. If arity > 0: 
			// - each input must be a column ref or a value
			// - the types of the inputs must be supported by the operator
			if (operatorType.getArity () > 0) {
				final Type[] types = new Type[inputs.size ()];
				for (int i = 0; i < types.length; ++ i) {
					final FilterExpression input = inputs.get (i);
					
					if (input instanceof ColumnReferenceExpression) {
						types[i] = ((ColumnReferenceExpression) input).getColumn().getDataType ();
					} else if (input instanceof ValueExpression) {
						types[i] = ((ValueExpression) input).getValueType ();
					} else {
						throw new IllegalArgumentException ("Found operator expression while expecting column reference or value");
					}
				}
				
				if (!validInputs.contains (new OperatorInput (operatorType, types))) {
					throw new IllegalArgumentException ("Invalid types for operator " + operatorType);
				}
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
				final @JsonProperty ("valueType") Type valueType,
				final @JsonProperty ("value") String value) {
			
			if (valueType == null) {
				throw new NullPointerException ("valueType cannot be null");
			}
			if (value == null) {
				throw new NullPointerException ("value cannot be null");
			}

			// Validate the value:
			switch (valueType) {
			case DATE:
				if (!value.matches ("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
					throw new IllegalArgumentException ("Invalid date: " + value);
				}
				break;
			case NUMERIC:
				if (!value.matches ("^(\\-)?\\d+(\\.\\d+)?$")) {
					throw new IllegalArgumentException ("Invalid number: " + value);
				}
				break;
			case GEOMETRY:
				throw new IllegalArgumentException ("Cannot create a value of type geometry");			
			case TEXT:
				break;
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
	
	private final static class OperatorInput {
		private final OperatorType operatorType;
		private final Set<Type> types = new HashSet<> ();
		
		public OperatorInput (final OperatorType operatorType, final Type ... types) {
			this.operatorType = operatorType;
			for (final Type type: types) {
				this.types.add (type);
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((operatorType == null) ? 0 : operatorType.hashCode());
			result = prime * result + ((types == null) ? 0 : types.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OperatorInput other = (OperatorInput) obj;
			if (operatorType != other.operatorType)
				return false;
			if (types == null) {
				if (other.types != null)
					return false;
			} else if (!types.equals(other.types))
				return false;
			return true;
		}
	}
}
