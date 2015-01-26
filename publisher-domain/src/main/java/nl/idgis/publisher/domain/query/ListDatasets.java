package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Dataset;

public class ListDatasets implements DomainQuery<Page<Dataset>>{

	private static final long serialVersionUID = -5582949253434171325L;
	
	private final String categoryId;
	private final long page;
	
	public ListDatasets (final Category category, long page) {
		this.categoryId = category == null ? null : category.id ();
		this.page = page;
	}
	
	public String categoryId () {
		return this.categoryId;
	}
	
	public long getPage() {
		return page;
	}

	@Override
	public String toString() {
		return "ListDatasets [categoryId=" + categoryId + ", page=" + page
				+ "]";
	}
}
 