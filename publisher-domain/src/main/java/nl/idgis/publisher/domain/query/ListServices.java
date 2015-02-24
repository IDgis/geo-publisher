package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Service;

public class ListServices implements DomainQuery<Page<Service>> {
	private static final long serialVersionUID = -2138467676079818345L;
	
	private final Long page;
	private final String query;
	private final Boolean published;
	
	public ListServices (final Long page, final String query, final Boolean published) {
		this.page = page;
		this.query = query;
		this.published = published;
	}

	public Long getPage () {
		return page;
	}

	public String getQuery () {
		return query;
	}

	public Boolean getPublished () {
		return published;
	}
}
