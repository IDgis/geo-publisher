package nl.idgis.publisher.protocol.messages;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

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
		StringWriter sw = new StringWriter();
		cause.printStackTrace(new PrintWriter(sw));
		
		return "Failure [cause=" + sw.toString() + "]";
	}
}
