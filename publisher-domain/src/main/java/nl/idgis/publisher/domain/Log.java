package nl.idgis.publisher.domain;

import java.io.Serializable;

import nl.idgis.publisher.domain.job.LogLevel;

public class Log implements Serializable {

	private static final long serialVersionUID = 6504302762477764168L;

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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Log other = (Log) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (level != other.level)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Log [level=" + level + ", type=" + type + ", content="
				+ content + "]";
	}
}
