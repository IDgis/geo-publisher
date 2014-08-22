package nl.idgis.publisher.service.rest;

import java.util.List;

public class FeatureType {
	
	private final String name, nativeName;
	private final List<Attribute> attributes;
	
	public FeatureType(String name) {
		this(name, null);
	}
	
	public FeatureType(String name, String nativeName) {
		this(name, nativeName, null);
	}
	
	public FeatureType(String name, String nativeName, List<Attribute> attributes) {
		this.name = name;
		this.nativeName = nativeName;
		this.attributes = attributes;
	}

	public String getName() {
		return name;
	}

	public String getNativeName() {
		return nativeName;
	}
	
	public List<Attribute> getAttributes() {
		return attributes;
	}
}
