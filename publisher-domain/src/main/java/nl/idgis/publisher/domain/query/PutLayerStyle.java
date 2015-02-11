package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Response;

public class PutLayerStyle implements DomainQuery<Response<?>>{

	private static final long serialVersionUID = -816217513624403962L;

	private final String layerId;
	private final String styleId;
	
	public PutLayerStyle (final String layerId, final String styleId) {
		this.layerId = layerId;
		this.styleId = styleId;
	}
	
	public String layerId () {
		return this.layerId;
	}

	public String styleId() {
		return styleId;
	}
	
}
 