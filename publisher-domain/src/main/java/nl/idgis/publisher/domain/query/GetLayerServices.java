package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.web.tree.Service;

public class GetLayerServices implements DomainQuery<List<String>>{

	private static final long serialVersionUID = 5921431566389327227L;

	private final String layerId;
	
	public GetLayerServices (final String layerId) {
		this.layerId = layerId;
	}
	
	public String layerId () {
		return this.layerId;
	}
	
}
 