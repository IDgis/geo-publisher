package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LayerGroup;

public class GetLayerParentGroups implements DomainQuery<Page<LayerGroup>> {
	
	private static final long serialVersionUID = -8188832261656007582L;
	
	private final String layerId;
	
	public GetLayerParentGroups (final String layerId) {
		this.layerId = layerId;
	}

	public String getId() {
		return layerId;
	}
	
}