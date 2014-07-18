package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

public class GetMessagePackager implements Serializable {
	
	private static final long serialVersionUID = 6037388337811836045L;
	
	private final String targetName;
	
	public GetMessagePackager(String targetName) {
		this.targetName = targetName;		
	}

	public String getTargetName() {
		return targetName;
	}	

	@Override
	public String toString() {
		return "GetMessagePackager [targetName=" + targetName + "]";
	}
}
