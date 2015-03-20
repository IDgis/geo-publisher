package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class GetDatasetLayerRef implements Serializable {

	private static final long serialVersionUID = -5806807222152581144L;
	
	private final String layerId;
	
	public GetDatasetLayerRef(String layerId) {
		this.layerId = layerId;
	}

	public String getLayerId() {
		return layerId;
	}

	@Override
	public String toString() {
		return "GetDatasetLayerRef [layerId=" + layerId + "]";
	}
	
}
