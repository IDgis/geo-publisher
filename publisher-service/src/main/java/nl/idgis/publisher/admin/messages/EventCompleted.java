package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class EventCompleted<T> implements Serializable {	

	private static final long serialVersionUID = -5814315334231935037L;
	
	private final T value;
	
	public EventCompleted(T value) {
		this.value = value;
	}
	
	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "EventCompleted [value=" + value + "]";
	}
}
