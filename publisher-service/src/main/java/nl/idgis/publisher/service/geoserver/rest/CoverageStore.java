package nl.idgis.publisher.service.geoserver.rest;

import java.net.URL;

public class CoverageStore {
	
	private final String name;

	private final URL url;
	
	public CoverageStore(String name, URL url) {
		this.name = name;
		this.url = url;
	}
	
	public String getName() {
		return name;
	}
	
	public URL getUrl() {
		return url;
	}
}
