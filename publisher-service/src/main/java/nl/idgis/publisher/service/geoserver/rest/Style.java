package nl.idgis.publisher.service.geoserver.rest;

import org.w3c.dom.Document;

public class Style {

	private final String styleId;
	
	private final Document sld;
	
	public Style(String styleId, Document sld) {
		this.styleId = styleId;
		this.sld = sld;
	}

	public String getStyleId() {
		return styleId;
	}

	public Document getSld() {
		return sld;
	}

	@Override
	public String toString() {
		return "Style [styleId=" + styleId + ", sld=" + sld + "]";
	}
}
