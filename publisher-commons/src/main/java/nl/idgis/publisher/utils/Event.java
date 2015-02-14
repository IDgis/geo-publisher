package nl.idgis.publisher.utils;

import java.io.Serializable;

public class Event implements Serializable {
	
	private static final long serialVersionUID = -6186802738448923506L;
	
	private final Object msg;
	
	public Event(Object msg) {
		this.msg = msg;
	}
	
	public Object getMessage() {
		return msg;
	}

	@Override
	public String toString() {
		return "Event [msg=" + msg + "]";
	}
}
