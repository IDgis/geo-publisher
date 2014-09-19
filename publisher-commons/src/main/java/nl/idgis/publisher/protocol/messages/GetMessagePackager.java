package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

public class GetMessagePackager implements Serializable {
	
	private static final long serialVersionUID = 6037388337811836045L;
	
	private final String targetName;
	private final boolean persistent;
	
	public GetMessagePackager(String targetName) {
		this(targetName, true);
	}
	
	public GetMessagePackager(String targetName, boolean persistent) {
		this.targetName = targetName;
		this.persistent = persistent;
	}

	public String getTargetName() {
		return targetName;
	}
	
	public boolean getPersistent() {
		return persistent;
	}

	@Override
	public String toString() {
		return "GetMessagePackager [targetName=" + targetName + ", persistent="
				+ persistent + "]";
	}	
}
