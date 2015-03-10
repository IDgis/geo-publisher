package nl.idgis.publisher.admin.messages;

import java.io.Serializable;

public class Event implements Serializable {

	private static final long serialVersionUID = 6817213536394652765L;

	private final Object request, response;	
		
	public Event(Object request, Object response) {
		this.request = request;
		this.response = response;
	}

	public Object getRequest() {
		return request;
	}

	public Object getResponse() {
		return response;
	}

	@Override
	public String toString() {
		return "Event [request=" + request + ", response=" + response + "]";
	}
}
