package nl.idgis.publisher.service.geoserver.rest;

import java.util.Optional;

public class LayerRef extends PublishedRef {	
	
	private final String styleName;
	
	public LayerRef(String layerName) {
		this(layerName, null);
	}
		
	public LayerRef(String layerName, String styleName) {
		super(layerName);
		
		this.styleName = styleName;
	}
	
	public boolean isGroup() {
		return false;
	}
	
	public LayerRef asLayerRef() {
		return this;
	}
	
	public Optional<String> getStyleName() {
		return Optional.ofNullable(styleName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((styleName == null) ? 0 : styleName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerRef other = (LayerRef) obj;
		if (styleName == null) {
			if (other.styleName != null)
				return false;
		} else if (!styleName.equals(other.styleName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "LayerRef [styleName=" + styleName + ", layerName=" + layerName
				+ "]";
	}
	
}
