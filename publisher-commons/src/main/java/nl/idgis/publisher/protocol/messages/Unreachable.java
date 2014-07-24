package nl.idgis.publisher.protocol.messages;


public class Unreachable extends Message {
	
	private static final long serialVersionUID = 8727161192928993324L;
	
	private final String causeMessage;

	public Unreachable(String targetName, String causeMessage) {
		super(targetName);
		
		this.causeMessage = causeMessage;
	}
	
	public String getMessageCause() {
		return causeMessage;
	}

	@Override
	public String toString() {
		return "Unreachable [causeMessage=" + causeMessage + ", targetName=" + targetName
				+ "]";
	}
	
}
