package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class FeatureType extends Dataset {
	
	private final List<Attribute> attributes;
	
	public FeatureType(String name, String nativeName, String title, String abstr, List<String> keywords, List<Attribute> attributes) {
		super(name, nativeName, title, abstr, keywords);
		
		this.attributes = attributes;
	}	
	
	public List<Attribute> getAttributes() {
		return attributes;
	}
}
