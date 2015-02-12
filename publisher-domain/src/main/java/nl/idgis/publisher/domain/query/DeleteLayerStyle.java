package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Response;

public class DeleteLayerStyle implements DomainQuery<Response<?>>{

	private static final long serialVersionUID = -844599722172489885L;

	private final String layerId;
	private final String styleId;
	
	public DeleteLayerStyle (final String layerId, final String styleId) {
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
 