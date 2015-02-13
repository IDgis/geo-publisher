package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.response.Response;

public class PutLayerStyles implements DomainQuery<Response<?>>{

	private static final long serialVersionUID = -816217513624403962L;

	private final String layerId;
	private final List<String> styleIds;
	
	public PutLayerStyles (final String layerId, final List<String> styleIds) {
		this.layerId = layerId;
		this.styleIds = styleIds;
	}
	
	public String layerId () {
		return this.layerId;
	}

	public List<String> styleIdList() {
		return styleIds;
	}
	
}
 