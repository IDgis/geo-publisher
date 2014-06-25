package nl.idgis.publisher.protocol;

import java.io.Serializable;

public class Message implements Serializable {	
	
	private static final long serialVersionUID = -6624873515941732598L;
	
	private final String targetName, sourceName;
	private final Object content;
	
	public Message(String targetName, Object content, String sourceName) {
		this.targetName = targetName;
		this.content = content;
		this.sourceName = sourceName;
	}
	
	public String getTargetName() {
		return targetName;		
	}
	
	public Object getContent() {
		return content;
	}
	
	public String getSourceName() {
		return sourceName;
	}

	@Override
	public String toString() {
		return "Message [targetName=" + targetName + ", sourceName="
				+ sourceName + ", content=" + content + "]";
	}
	
}
