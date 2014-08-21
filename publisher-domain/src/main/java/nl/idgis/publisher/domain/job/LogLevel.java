package nl.idgis.publisher.domain.job;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum LogLevel {

	DEBUG,
	INFO,
	WARNING,
	ERROR;
	
	public Set<LogLevel> andUp() {
		Set<LogLevel> retval = new HashSet<>();
		
		for(LogLevel logLevel : LogLevel.values()) {
			if(logLevel.ordinal() >= ordinal()) {
				retval.add(logLevel);
			}
		}
		
		return Collections.unmodifiableSet(retval);
	}
}
