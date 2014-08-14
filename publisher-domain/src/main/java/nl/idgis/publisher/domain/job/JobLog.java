package nl.idgis.publisher.domain.job;

import java.io.Serializable;

public abstract class JobLog implements Serializable {
	
	private static final long serialVersionUID = 7393703558996721495L;
	
	protected final JobState state;
	protected final LogLevel level;
	protected final Enum<?> type;
	
	protected JobLog(JobState state, LogLevel level, Enum<?> type) {
		this.state = state;
		this.level = level;
		this.type = type;
	}
	public JobState getState() {
		return state;
	}
	
	public LogLevel getLevel() {
		return level;
	}
	
	public Enum<?> getEvent() {
		return type;
	}
}
