package nl.idgis.publisher.service.geoserver.rest;

public abstract class PublishedRef {
	
	private final String layerName;
	
	protected PublishedRef(String layerName) {
		this.layerName = layerName;
	}

	public abstract boolean isGroup();
	
	public LayerRef asLayerRef() {
		throw new IllegalArgumentException("not a LayerRef");
	}
	
	public GroupRef asGroupRef() {
		throw new IllegalArgumentException("not a GroupRef");
	}
	
	public String getLayerName() {
		return layerName;
	}
}
