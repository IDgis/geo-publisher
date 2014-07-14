package nl.idgis.publisher.protocol;

public class Envelope extends Message {	
	
	private static final long serialVersionUID = -6624873515941732598L;
	
	private final String sourceName;
	private final Object content;
	
	public Envelope(String targetName, Object content, String sourceName) {
		super(targetName);
		
		this.content = content;
		this.sourceName = sourceName;
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
