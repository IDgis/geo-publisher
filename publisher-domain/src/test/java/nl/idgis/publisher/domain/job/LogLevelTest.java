package nl.idgis.publisher.domain.job;

import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogLevelTest {

	@Test
	public void testAndUp() {
		Set<LogLevel> debugAndUp = LogLevel.DEBUG.andUp();
		assertEquals("wrong number of log levels", 4, debugAndUp.size());
		assertTrue("LogLevel.DEBUG missing", debugAndUp.contains(LogLevel.DEBUG));
		assertTrue("LogLevel.INFO missing", debugAndUp.contains(LogLevel.INFO));
		assertTrue("LogLevel.WARNING missing", debugAndUp.contains(LogLevel.WARNING));
		assertTrue("LogLevel.ERROR missing", debugAndUp.contains(LogLevel.ERROR));
		
		Set<LogLevel> errorAndUp = LogLevel.ERROR.andUp();
		assertEquals("wrong number of log levels", 1, errorAndUp.size());
		assertTrue("LogLevel.ERROR missing", errorAndUp.contains(LogLevel.ERROR));
	}
}
