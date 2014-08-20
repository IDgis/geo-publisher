package nl.idgis.publisher.domain.job;

import java.io.Serializable;

import nl.idgis.publisher.domain.MessageType;

public class JobLog implements Serializable {
	
	private static final long serialVersionUID = 7393703558996721495L;
	
	protected final LogLevel level;
	protected final MessageType type;
	protected final Object content;
	
	protected JobLog(LogLevel level, MessageType type) {
		this(level, type, null);
	}
	
	public JobLog(LogLevel level, MessageType type, Object content) {	
		this.level = level;
		this.type = type;
		this.content = content;
	}
	
	public LogLevel getLevel() {
		return level;
	}
	
	public MessageType getType() {
		return type;
	}
	
	public Object getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "JobLog [level=" + level + ", type=" + type + ", content="
				+ content + "]";
	}
}
