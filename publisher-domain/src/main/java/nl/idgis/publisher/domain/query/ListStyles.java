package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.web.Style;

public class ListStyles implements DomainQuery<Page<Style>> {

	private static final long serialVersionUID = 962251380025677687L;
	
	private final Long page;
	private final String query;
	
	public ListStyles (final Long page, final String query) {
		this.page = page;
		this.query = query;
	}

	public Long getPage() {
		return page;
	}

	public String getQuery() {
		return query;
	}
}
