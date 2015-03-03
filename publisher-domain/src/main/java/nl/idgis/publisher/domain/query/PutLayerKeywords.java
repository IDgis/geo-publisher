package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.response.Response;

public class PutLayerKeywords implements DomainQuery<Response<?>>{

	private static final long serialVersionUID = -2380672390462922110L;

	private final String layerId;
	private final List<String> keywords;
	
	public PutLayerKeywords (final String layerId, final List<String> keywords) {
		this.layerId = layerId;
		this.keywords = keywords;
	}
	
	public String layerId () {
		return this.layerId;
	}

	public List<String> keywordList() {
		return keywords;
	}
	
}
 