package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.web.Style;

public class ListLayerKeywords implements DomainQuery<List<String>>{

	private static final long serialVersionUID = -5582949253434171325L;
	
	private final String layerId;
	
	public ListLayerKeywords (final String layerId) {
		this.layerId = layerId;
	}
	
	public String layerId () {
		return this.layerId;
	}
	
}
 