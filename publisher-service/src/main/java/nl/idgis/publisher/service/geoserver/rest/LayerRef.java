package nl.idgis.publisher.service.geoserver.rest;

public class LayerRef {

	private final String layerId;
	
	private final boolean group;
	
	public LayerRef(String layerId, boolean group) {
		this.layerId = layerId;
		this.group = group;
	}
	
	public String getLayerId() {
		return layerId;
	}
	
	public boolean isGroup() {
		return group;
	}

	@Override
	public String toString() {
		return "LayerRef [layerId=" + layerId + ", group=" + group + "]";
	}
}
