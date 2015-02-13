package nl.idgis.publisher.service.geoserver.rest;

import java.util.List;

public class FeatureType {
	
	private final String name, nativeName, title, abstr;
	
	private final List<Attribute> attributes;
	
	public FeatureType(String name, String nativeName, String title, String abstr) {
		this(name, nativeName, title, abstr, null);
	}
	
	public FeatureType(String name, String nativeName, String title, String abstr, List<Attribute> attributes) {
		this.name = name;		
		this.nativeName = nativeName;
		this.title = title;
		this.abstr = abstr;
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
	
	public List<Attribute> getAttributes() {
		return attributes;
	}
}
