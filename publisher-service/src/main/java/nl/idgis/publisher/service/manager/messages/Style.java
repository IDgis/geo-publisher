package nl.idgis.publisher.service.manager.messages;

import org.w3c.dom.Document;

import nl.idgis.publisher.stream.messages.Item;

public class Style extends Item {	

	private static final long serialVersionUID = 3913869022340793843L;

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
