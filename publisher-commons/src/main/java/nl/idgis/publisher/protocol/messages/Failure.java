package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

public class Failure implements Serializable {
	
	private static final long serialVersionUID = -7132830504795101059L;
	
	private final Throwable cause;
	
	public Failure(Throwable cause) {
		this.cause = cause;
	}
	
	public Throwable getCause() {
		return cause;
	}

	@Override
	public String toString() {
		return "Failure [cause=" + cause + "]";
	}
}
