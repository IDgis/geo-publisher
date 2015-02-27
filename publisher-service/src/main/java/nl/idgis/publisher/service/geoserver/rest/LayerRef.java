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
	
}
