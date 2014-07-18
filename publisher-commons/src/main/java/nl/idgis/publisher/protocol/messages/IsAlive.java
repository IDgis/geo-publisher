package nl.idgis.publisher.protocol.messages;


public class IsAlive extends Message {
	
	private static final long serialVersionUID = 6562661654351249356L;

	public IsAlive(String targetName) {
		super(targetName);
	}

	@Override
	public String toString() {
		return "IsAlive [targetName=" + targetName + "]";
	}
}
