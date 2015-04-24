package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

import org.w3c.dom.Document;

public class Style implements Serializable {
	
	private static final long serialVersionUID = 2837719446303072319L;

	private final String styleName;

	private final Document sld;
	
	public Style(String styleName, Document sld) {
		this.styleName = styleName;
		this.sld = sld;
	}

	public String getStyleName() {
		return styleName;
	}

	public Document getSld() {
		return sld;
	}

	@Override
	public String toString() {
		return "Style [styleName=" + styleName + "]";
	}
}
