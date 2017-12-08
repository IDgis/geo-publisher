package nl.idgis.publisher.console;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jolokia.client.J4pClient;
import org.jolokia.client.exception.J4pException;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pListRequest;
import org.jolokia.client.request.J4pReadRequest;

public class LogbackContext {
	
	private static final String LOGBACK_PATH = "ch.qos.logback.classic";

	private J4pClient client;
	
	private final String objectName;
	
	public static LogbackContext getSingleLogbackContext(J4pClient client) throws J4pException {
		Objects.requireNonNull(client, "client should not be null");
		
		Map<String, Object> logbacks = client.execute(new J4pListRequest(LOGBACK_PATH)).getValue();
		
		if(logbacks.size() != 1) {
			throw new IllegalStateException("expected: a single logback context");
		}
		
		return new LogbackContext(client, LOGBACK_PATH + ":" + logbacks.keySet().iterator().next());
	}
	
	public LogbackContext(J4pClient client, String objectName) {
		this.client = Objects.requireNonNull(client, "client should not be null");
		this.objectName = Objects.requireNonNull(objectName, "objectName should not be null"); 
	}
	
	public List<String> getLoggerList() {
		try {
			return client.execute(new J4pReadRequest(objectName, "LoggerList")).getValue();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getLoggerLevel(String logger) {
		requireNonBlank(logger);
		
		try {
			return client.execute(new J4pExecRequest(objectName, "getLoggerLevel", logger)).getValue().toString();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setLoggerLevel(String logger, String logLevel) {
		requireNonBlank(logger);
		requireNonBlank(logLevel);
		
		try {
			client.execute(new J4pExecRequest(objectName, "setLoggerLevel", logger, logLevel));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String requireNonBlank(String logger) {
		Objects.requireNonNull(logger, "logger should not be null");
		if(logger.trim().isEmpty()) {
			throw new IllegalArgumentException("logger should not be blank");
		}
		
		return logger;
	}
}
