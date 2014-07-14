package nl.idgis.publisher.protocol;

public class Unreachable extends Message {
	
	private static final long serialVersionUID = 8727161192928993324L;
	
	private final Throwable cause;

	public Unreachable(String targetName, Throwable cause) {
		super(targetName);
		
		this.cause = cause;
	}
	
	public Throwable getCause() {
		return cause;
	}

	@Override
	public String toString() {
		return "Unreachable [cause=" + cause + ", targetName=" + targetName
				+ "]";
	}
	
}
