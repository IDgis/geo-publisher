package nl.idgis.publisher.service.metadata.messages;

import java.io.Serializable;

public class NotValid<T> implements Serializable {
	
	private static final long serialVersionUID = 994139552063884067L;
	
	private final String expression;
	private final T value;
	
	public NotValid(String expression, T value) {
		this.expression = expression;
		this.value = value;
	}
	
	public String getExpression() {
		return expression;
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "NotValid [expression=" + expression + ", value=" + value + "]";
	}
	
}
