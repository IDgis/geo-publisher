package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.response.Response;

public class PutServiceKeywords implements DomainQuery<Response<?>>{

	private static final long serialVersionUID = 1973064041352395844L;

	private final String serviceId;
	private final List<String> keywords;
	
	public PutServiceKeywords (final String serviceId, final List<String> keywords) {
		this.serviceId = serviceId;
		this.keywords = keywords;
	}
	
	public String serviceId () {
		return this.serviceId;
	}

	public List<String> keywordList() {
		return keywords;
	}
	
}
 