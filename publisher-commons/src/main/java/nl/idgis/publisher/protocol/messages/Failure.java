package nl.idgis.publisher.protocol.messages;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;

public class Failure implements Serializable {
	
	private static final long serialVersionUID = -7132830504795101059L;
	
	private final Collection<Failure> failures;
	
	private final Throwable cause;
	
	public Failure(Collection<Failure> failures) {
		this.failures = failures;
		this.cause = null;
	}
	
	public Failure(Throwable cause) {
		this.failures = null;
		this.cause = cause;
	}
	
	public Throwable getCause() {
		return cause;
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		sw.append("Failure [");
		
		if(cause == null) {
			sw.append("failures=[");			
			failures.forEach(failure ->
				sw.append("\n\t" + failure));
		} else {
			sw.append("cause=");
			cause.printStackTrace(new PrintWriter(sw));
		}
		
		sw.append("]");
		
		return sw.toString();
	}
}
