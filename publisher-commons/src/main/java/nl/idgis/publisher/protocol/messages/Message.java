package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

public abstract class Message implements Serializable {
	
	private static final long serialVersionUID = 2640413079987990030L;
	
	protected final String targetName;
	
	public Message(String targetName) {
		this.targetName = targetName;
	}
	
	public String getTargetName() {
		return targetName;
	}
}
