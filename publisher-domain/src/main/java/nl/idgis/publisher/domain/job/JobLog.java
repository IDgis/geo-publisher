package nl.idgis.publisher.domain.job;

import java.io.Serializable;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;

public class JobLog implements Serializable {
	
	private static final long serialVersionUID = 7393703558996721495L;
	
	protected final LogLevel level;
	protected final MessageType<?> type;
	protected final MessageProperties content;
	
	protected JobLog(LogLevel level, MessageType<?> type) {
		this(level, type, null);
	}
	
	protected JobLog(LogLevel level, MessageType<?> type, MessageProperties content) {	
		this.level = level;
		this.type = type;
		this.content = content;
	}
	
	public static <T extends MessageProperties> JobLog create (final LogLevel level, final MessageType<T> type, final T content) {
		return new JobLog (level, type, content);
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
