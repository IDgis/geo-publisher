package nl.idgis.publisher.protocol.messages;

public class Destroyed extends Message {
	
	private static final long serialVersionUID = 7279691746837532306L;

	public Destroyed(String targetName) {
		super(targetName);
	}

	@Override
	public String toString() {
		return "Destroyed [targetName=" + targetName + "]";
	}
}
