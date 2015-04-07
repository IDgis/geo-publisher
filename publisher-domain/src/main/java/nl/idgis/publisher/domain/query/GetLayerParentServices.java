package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Service;

public class GetLayerParentServices implements DomainQuery<Page<Service>> {
	
	private static final long serialVersionUID = 3536920774951711434L;
	
	private final String layerId;
	
	public GetLayerParentServices (final String layerId) {
		this.layerId = layerId;
	}

	public String getId() {
		return layerId;
	}
	
}