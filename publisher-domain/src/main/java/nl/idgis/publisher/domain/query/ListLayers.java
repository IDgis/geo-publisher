package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Layer;

public class ListLayers implements DomainQuery<Page<Layer>> {
	private static final long serialVersionUID = -1187299229753974752L;
	
	private final Long page;
	private final String query;
	private final String datasetId;
	
	public ListLayers (final Long page, final String query) {
		this (page, query, null);
	}
	
	public ListLayers (final Long page, final String query, final String datasetId) {
		this.page = page;
		this.query = query;
		this.datasetId = datasetId;
	}

	public Long getPage () {
		return page;
	}

	public String getQuery () {
		return query;
	}

	public String getDatasetId () {
		return datasetId;
	}
}
