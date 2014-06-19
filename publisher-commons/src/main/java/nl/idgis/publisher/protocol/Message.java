package nl.idgis.publisher.protocol;

import java.io.Serializable;

public class Message implements Serializable {	
	
	private static final long serialVersionUID = -6624873515941732598L;
	
	private final String targetName;
	private final Object content;
	
	public Message(String targetName, Object content) {
		this.targetName = targetName;
		this.content = content;
	}
	
	public String getTargetName() {
		return targetName;		
	}
	
	public Object getContent() {
		return content;
	}
}
