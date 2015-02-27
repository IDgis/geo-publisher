package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Layer;

public class ListLayers implements DomainQuery<Page<Layer>> {
	private static final long serialVersionUID = -1187299229753974752L;
	
	private final Long page;
	private final String query;
	private final Boolean published;
	
	public ListLayers (final Long page, final String query, final Boolean published) {
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
