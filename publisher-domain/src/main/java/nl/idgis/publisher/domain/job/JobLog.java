package nl.idgis.publisher.domain.job;

import java.io.Serializable;

public class JobLog implements Serializable {
	
	private static final long serialVersionUID = 7393703558996721495L;
	
	protected final LogLevel level;
	protected final Enum<?> type;
	protected final Object content;
	
	protected JobLog(LogLevel level, Enum<?> type) {
		this(level, type, null);
	}
	
	public JobLog(LogLevel level, Enum<?> type, Object content) {	
		this.level = level;
		this.type = type;
		this.content = content;
	}
	
	public LogLevel getLevel() {
		return level;
	}
	
	public Enum<?> getType() {
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
