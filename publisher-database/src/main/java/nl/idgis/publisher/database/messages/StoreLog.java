package nl.idgis.publisher.database.messages;

import nl.idgis.publisher.domain.log.LogLine;

public class StoreLog extends Query {

	private static final long serialVersionUID = 7563644866851810078L;
	
	private final LogLine logLine;
	
	public StoreLog(LogLine logLine) {
		this.logLine = logLine;
	}
	
	public LogLine getLogLine() {
		return logLine;
	}

	@Override
	public String toString() {
		return "StoreLog [logLine=" + logLine + "]";
	}
}
