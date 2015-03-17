package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.DatasetStatusType;

public class ListDatasets implements DomainQuery<Page<Dataset>>{

	private static final long serialVersionUID = -5582949253434171325L;
	
	private final String categoryId;
	
	private final DatasetStatusType status;
	
	private final long page;
	
	public ListDatasets (final Category category, DatasetStatusType status, long page) {
		this.categoryId = category == null ? null : category.id ();
		this.status = status;
		this.page = page;
	}
	
	public String categoryId () {
		return this.categoryId;
	}
	
	public DatasetStatusType status () {
		return this.status;
	}
	
	public long getPage() {
		return page;
	}

	@Override
	public String toString() {
		return "ListDatasets [categoryId=" + categoryId + ", status=" + status
				+ ", page=" + page + "]";
	}

}
 