package nl.idgis.publisher.service.geoserver.rest;

import java.util.Collections;
import java.util.List;

public class Layer {

	private final String name;
	
	private final StyleRef defaultStyle;
	
	private final List<StyleRef> additionalStyles;
	
	private final boolean queryable;
	
	public Layer(String name, StyleRef defaultStyle, List<StyleRef> additionalStyles, boolean queryable) {
		this.name = name;
		this.defaultStyle = defaultStyle;
		this.additionalStyles = additionalStyles == null ? Collections.emptyList() : additionalStyles;
		this.queryable = queryable;
	}

	public String getName() {
		return name;
	}

	public StyleRef getDefaultStyle() {
		return defaultStyle;
	}

	public List<StyleRef> getAdditionalStyles() {
		return additionalStyles;
	}

	public boolean isQueryable() {
		return queryable;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((additionalStyles == null) ? 0 : additionalStyles.hashCode());
		result = prime * result
				+ ((defaultStyle == null) ? 0 : defaultStyle.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Layer other = (Layer) obj;
		if (additionalStyles == null) {
			if (other.additionalStyles != null)
				return false;
		} else if (!additionalStyles.equals(other.additionalStyles))
			return false;
		if (defaultStyle == null) {
			if (other.defaultStyle != null)
				return false;
		} else if (!defaultStyle.equals(other.defaultStyle))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Layer [name=" + name + ", defaultStyle=" + defaultStyle
				+ ", additionalStyles=" + additionalStyles + "]";
	}
	
}
