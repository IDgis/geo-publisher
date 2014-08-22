package nl.idgis.publisher.service.rest;

import java.util.Map;

public class DataStore {
	
	private final Map<String, String> connectionParameters;
	private final String name;
	
	public DataStore(String name, Map<String, String> connectionParameters) {
		this.name = name;
		this.connectionParameters = connectionParameters;
	}
	
	public Map<String, String> getConnectionParameters() {
		return connectionParameters;
	}

	public String getName() {
		return name;
	}	
}
