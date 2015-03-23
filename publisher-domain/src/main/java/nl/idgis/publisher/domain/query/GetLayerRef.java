package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.tree.LayerRef;

public class GetLayerRef implements DomainQuery<LayerRef<?>> {	

	private static final long serialVersionUID = 1522796510045623274L;
	
	private final String layerId;

	public GetLayerRef(String layerId) {
		this.layerId = layerId;
	}

	public String getLayerId() {
		return layerId;
	}

	@Override
	public String toString() {
		return "GetLayerRef [layerId=" + layerId + "]";
	}
}
