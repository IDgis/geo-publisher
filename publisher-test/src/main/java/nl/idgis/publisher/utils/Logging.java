package nl.idgis.publisher.utils;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import org.mockito.stubbing.Answer;

import akka.event.LoggingAdapter;

public class Logging {
	
	private static void println(String level, Object[] args) {
		String message = args[0].toString();
		for(int i = 1; i < args.length; i++) {
			Object o = args[i];			
			message = message.replaceFirst("\\{\\}", o == null ? "null" : o.toString());
		}
		
		System.out.println("[" + level + "] [test] " + message);
	}

	public static LoggingAdapter getLogger() {
		LoggingAdapter log = mock(LoggingAdapter.class);
		
		Answer<Void> debugAnswer = invocation -> {
			println("DEBUG", invocation.getArguments());
			return null;
		};
		doAnswer(debugAnswer).when(log).debug(anyString());
		doAnswer(debugAnswer).when(log).debug(anyString(), anyObject());
		doAnswer(debugAnswer).when(log).debug(anyString(), anyObject(), anyObject());
		doAnswer(debugAnswer).when(log).debug(anyString(), anyObject(), anyObject(), anyObject());
		doAnswer(debugAnswer).when(log).debug(anyString(), anyObject(), anyObject(), anyObject(), anyObject());
		
		Answer<Void> warningAnswer = invocation -> {
			println("WARNING", invocation.getArguments());
			return null;
		};		
		doAnswer(warningAnswer).when(log).warning(anyString());
		doAnswer(warningAnswer).when(log).warning(anyString(), anyObject());
		doAnswer(warningAnswer).when(log).warning(anyString(), anyObject(), anyObject());
		doAnswer(warningAnswer).when(log).warning(anyString(), anyObject(), anyObject(), anyObject());
		doAnswer(warningAnswer).when(log).warning(anyString(), anyObject(), anyObject(), anyObject(), anyObject());
		
		Answer<Void> infoAnswer = invocation -> {
			println("INFO", invocation.getArguments());
			return null;
		};		
		doAnswer(infoAnswer).when(log).info(anyString());
		doAnswer(infoAnswer).when(log).info(anyString(), anyObject());
		doAnswer(infoAnswer).when(log).info(anyString(), anyObject(), anyObject());
		doAnswer(infoAnswer).when(log).info(anyString(), anyObject(), anyObject(), anyObject());
		doAnswer(infoAnswer).when(log).info(anyString(), anyObject(), anyObject(), anyObject(), anyObject());
		
		Answer<Void> errorAnswer = invocation -> {
			println("ERROR", invocation.getArguments());
			return null;
		};		
		doAnswer(errorAnswer).when(log).error(anyString());
		doAnswer(errorAnswer).when(log).error(anyString(), anyObject());
		doAnswer(errorAnswer).when(log).error(anyString(), anyObject(), anyObject());
		doAnswer(errorAnswer).when(log).error(anyString(), anyObject(), anyObject(), anyObject());
		doAnswer(errorAnswer).when(log).error(anyString(), anyObject(), anyObject(), anyObject(), anyObject());
		
		when(log.isDebugEnabled()).thenReturn(true);
		when(log.isWarningEnabled()).thenReturn(true);
		when(log.isInfoEnabled()).thenReturn(true);
		when(log.isErrorEnabled()).thenReturn(true);
		when(log.isEnabled(anyInt())).thenReturn(true);
		
		return log;
	}
}
