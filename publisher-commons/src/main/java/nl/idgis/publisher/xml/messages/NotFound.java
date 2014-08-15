package nl.idgis.publisher.xml.messages;

import java.io.Serializable;

public class NotFound implements Serializable {
	
	private static final long serialVersionUID = 3662946738782038740L;
	
	private final String expression;
	
	public NotFound(String expression) {
		this.expression = expression;
	}
	
	public String getExpression() {
		return expression;
	}

	@Override
	public String toString() {
		return "NotFound [expression=" + expression + "]";
	}	

}
