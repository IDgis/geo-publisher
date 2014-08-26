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
	
	public static Set<LogLevel> all () {
		final Set<LogLevel> allLogLevels = new HashSet<> ();
		
		allLogLevels.add (DEBUG);
		allLogLevels.add (INFO);
		allLogLevels.add (WARNING);
		allLogLevels.add (ERROR);
		
		return Collections.unmodifiableSet (allLogLevels);
	}
}
