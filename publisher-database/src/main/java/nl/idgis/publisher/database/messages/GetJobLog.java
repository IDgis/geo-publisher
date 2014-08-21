package nl.idgis.publisher.database.messages;

import scala.concurrent.duration.FiniteDuration;
import nl.idgis.publisher.domain.job.LogLevel;

import com.mysema.query.types.Order;

public class GetJobLog extends ListQuery {
	
	private static final long serialVersionUID = 8295803321912336531L;
	
	private static final Order DEFAULT_ORDER = Order.DESC;
	private static final Long DEFAULT_OFFSET = 0L;
	private static final Long DEFAULT_LIMIT = 5L;
	
	private final LogLevel logLevel;
	private final FiniteDuration maxAge;
	
	public GetJobLog(LogLevel logLevel) {
		this(logLevel, null);
	}
	
	public GetJobLog(LogLevel logLevel, FiniteDuration maxAge) {
		this(DEFAULT_ORDER, logLevel, maxAge);
	}
	
	public GetJobLog(Order order, LogLevel logLevel) {
		this(order, logLevel, null);
	}
	
	public GetJobLog(Order order, LogLevel logLevel, FiniteDuration maxAge) {
		this(order, DEFAULT_OFFSET, DEFAULT_LIMIT, logLevel, maxAge);
	}
	
	public GetJobLog(Long offset, Long limit, LogLevel logLevel) {
		this(offset, limit, logLevel, null);
	}
	
	public GetJobLog(Long offset, Long limit, LogLevel logLevel, FiniteDuration maxAge) {
		this(DEFAULT_ORDER, offset, limit, logLevel, maxAge);
	}
	
	public GetJobLog(Order order, Long offset, Long limit, LogLevel logLevel) {
		this(order, offset, limit, logLevel, null);
	}

	public GetJobLog(Order order, Long offset, Long limit, LogLevel logLevel, FiniteDuration maxAge) {
		super(order, offset, limit);
		
		this.logLevel = logLevel;
		this.maxAge = maxAge;
	}
	
	public LogLevel getLogLevel() {
		return logLevel;
	}
	
	public FiniteDuration getMaxAge() {
		return maxAge;
	}

	@Override
	public String toString() {
		return "GetJobLog [logLevel=" + logLevel + ", maxAge=" + maxAge + "]";
	}	
	
}
