package nl.idgis.publisher.protocol.stream;

import java.io.Serializable;

public class Failure implements Serializable {
	
	private static final long serialVersionUID = -7132830504795101059L;
	
	private final String message;
	
	public Failure(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "Error [message=" + message + "]";
	}
}
