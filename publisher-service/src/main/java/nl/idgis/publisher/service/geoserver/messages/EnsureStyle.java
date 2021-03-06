package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

import org.w3c.dom.Document;

import nl.idgis.publisher.service.geoserver.rest.Style;

public class EnsureStyle implements Serializable {	

	private static final long serialVersionUID = 2867197238385385472L;

	private final String name;
	
	private final Document sld;

	public EnsureStyle(String name, Document sld) {
		this.name = name;
		this.sld = sld;
	}

	public String getName() {
		return name;
	}

	public Document getSld() {
		return sld;
	}
	
	public Style getStyle() {
		return new Style(name, sld);
	}

	@Override
	public String toString() {
		return "EnsureStyle [name=" + name + ", sld=" + sld + "]";
	}
}
