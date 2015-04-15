package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LayerGroup;

public class ListLayerGroups implements DomainQuery<Page<LayerGroup>> {
	private static final long serialVersionUID = 2191222432798846543L;
	
	private final Long page;
	private final String query;

	public ListLayerGroups (final Long page, final String query) {
		this.page = page;
		this.query = query;
	}

	public Long getPage () {
		return page;
	}

	public String getQuery () {
		return query;
	}
}
