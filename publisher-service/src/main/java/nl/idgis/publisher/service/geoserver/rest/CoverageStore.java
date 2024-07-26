package nl.idgis.publisher.service.geoserver.rest;

import java.net.URL;

public class CoverageStore {
	
	private final String name;

	private final String url;
	
	public CoverageStore(String name, String url) {
		this.name = name;
		this.url = url;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUrl() {
		return url;
	}
}
