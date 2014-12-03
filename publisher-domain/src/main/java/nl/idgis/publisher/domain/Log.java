package nl.idgis.publisher.domain;

import java.io.Serializable;

import nl.idgis.publisher.domain.job.LogLevel;

public class Log implements Serializable {
	
	private static final long serialVersionUID = 7393703558996721495L;
	
	protected final LogLevel level;
	protected final MessageType<?> type;
	protected final MessageProperties content;
	
	protected Log(LogLevel level, MessageType<?> type) {
		this(level, type, null);
	}
	
	protected Log(LogLevel level, MessageType<?> type, MessageProperties content) {	
		this.level = level;
		this.type = type;
		this.content = content;
	}
	
	public static <T extends MessageProperties> Log create (final LogLevel level, final MessageType<T> type) {
		return new Log (level, type);
	}
	
	public static <T extends MessageProperties> Log create (final LogLevel level, final MessageType<T> type, final T content) {
		return new Log (level, type, content);
	}
	
	public LogLevel getLevel() {
		return level;
	}
	
	public MessageType<?> getType() {
		return type;
	}
	
	public MessageProperties getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "JobLog [level=" + level + ", type=" + type + ", content="
				+ content + "]";
	}
}
