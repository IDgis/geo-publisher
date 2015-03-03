package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.web.Style;

public class ListServiceKeywords implements DomainQuery<List<String>>{

	private static final long serialVersionUID = -402365130254059762L;

	private final String serviceId;
	
	public ListServiceKeywords (final String serviceId) {
		this.serviceId = serviceId;
	}
	
	public String serviceId () {
		return this.serviceId;
	}
	
}
 