package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class FeatureType {
	
	private final String name, nativeName, title, abstr;
	
	private final List<String> keywords;
	
	private final List<Attribute> attributes;
	
	public FeatureType(String name, String nativeName, String title, String abstr, List<String> keywords, List<Attribute> attributes) {
		this.name = name;		
		this.nativeName = nativeName;
		this.title = title;
		this.abstr = abstr;
		this.keywords = keywords;
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public String getNativeName() {
		return nativeName;
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
	
	public List<Attribute> getAttributes() {
		return attributes;
	}
}
