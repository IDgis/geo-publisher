package nl.idgis.publisher.database.messages;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import nl.idgis.publisher.domain.job.LogLevel;

import org.joda.time.LocalDateTime;

import com.mysema.query.types.Order;

public class GetJobLog extends ListQuery {
	
	private static final long serialVersionUID = 8295803321912336531L;
	
	private static final Order DEFAULT_ORDER = Order.DESC;
	private static final Long DEFAULT_OFFSET = 0L;
	private static final Long DEFAULT_LIMIT = 5L;
	
	private final Set<LogLevel> logLevels;
	private final LocalDateTime since;
	
	public GetJobLog (final LogLevel baseLogLevel) {
		this (baseLogLevel.andUp ());
	}
	
	public GetJobLog(Set<LogLevel> logLevels) {
		this(logLevels, null);
	}
	
	public GetJobLog(Set<LogLevel> logLevels, LocalDateTime since) {
		this(DEFAULT_ORDER, logLevels, since);
	}
	
	public GetJobLog(Order order, Set<LogLevel> logLevels) {
		this(order, logLevels, null);
	}
	
	public GetJobLog(Order order, Set<LogLevel> logLevels, LocalDateTime since) {
		this(order, DEFAULT_OFFSET, DEFAULT_LIMIT, logLevels, since);
	}
	
	public GetJobLog(Long offset, Long limit, Set<LogLevel> logLevels) {
		this(offset, limit, logLevels, null);
	}
	
	public GetJobLog(Long offset, Long limit, Set<LogLevel> logLevels, LocalDateTime since) {
		this(DEFAULT_ORDER, offset, limit, logLevels, since);
	}
	
	public GetJobLog(Order order, Long offset, Long limit, Set<LogLevel> logLevels) {
		this(order, offset, limit, logLevels, null);
	}

	public GetJobLog(Order order, Long offset, Long limit, Set<LogLevel> logLevels, LocalDateTime since) {
		super(order, offset, limit);
		
		this.logLevels = new HashSet<> (logLevels == null ? LogLevel.all () : logLevels);
		this.since = since;
	}
	
	
	public Set<LogLevel> getLogLevels() {
		return Collections.unmodifiableSet (logLevels);
	}
	
	public LocalDateTime getSince() {
		return since;
	}

	@Override
	public String toString() {
		return "GetJobLog [logLevels=" + logLevels + ", since=" + since + "]";
	}	
	
}
