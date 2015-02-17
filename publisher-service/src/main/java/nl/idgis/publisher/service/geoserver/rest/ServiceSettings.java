package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class ServiceSettings {

	private final String title, abstr;
	
	private final List<String> keywords;
	
	public ServiceSettings(String title, String abstr, List<String> keywords) {
		this.title = title;
		this.abstr = abstr;
		this.keywords = keywords;
	}

	public String getTitle() {
		return title;
	}
	
	public String getAbstract() {
		return abstr;
	}
	
	public List<String> getKeywords() {
		return keywords;
	}
}
