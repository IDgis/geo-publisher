package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Style;

public class ListStyles implements DomainQuery<Page<Style>> {

	private static final long serialVersionUID = 962251380025677687L;
	
	private final Long page;
	private final String query;
	private final String styleType;
	
	public ListStyles (final Long page, final String query, final String styleType) {
		this.page = page;
		this.query = query;
		this.styleType = styleType;
	}

	public Long getPage() {
		return page;
	}

	public String getQuery() {
		return query;
	}
	
	public String getStyleType() {
		return styleType;
	}
}
