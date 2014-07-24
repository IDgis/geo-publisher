package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

public class StopPackager implements Serializable {
	
	private static final long serialVersionUID = 4651656464899923994L;
	
	private final String targetName;
	
	public StopPackager(String targetName) {
		this.targetName = targetName;
	}
	
	public String getTargetName() {
		return targetName;
	}

	@Override
	public String toString() {
		return "StopPackager [targetName=" + targetName + "]";
	}
}
