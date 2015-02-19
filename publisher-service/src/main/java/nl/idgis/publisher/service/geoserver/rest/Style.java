package nl.idgis.publisher.service.geoserver.rest;

import org.w3c.dom.Document;

public class Style {

	private final String name;
	
	private final Document sld;
	
	public Style(String name, Document sld) {
		this.name = name;
		this.sld = sld;
	}

	public String getName() {
		return name;
	}

	public Document getSld() {
		return sld;
	}

	@Override
	public String toString() {
		return "Style [name=" + name + ", sld=" + sld + "]";
	}
}
