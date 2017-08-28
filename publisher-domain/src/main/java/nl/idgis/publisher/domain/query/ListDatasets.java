package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.DatasetStatusType;

public class ListDatasets implements DomainQuery<Page<Dataset>>{
	
	private static final long serialVersionUID = -6774849832776159562L;
	
	private final String categoryId;
	
	private final DatasetStatusType status;
	
	private final Boolean withCoupling;
	
	private final long page;
	
	private final String query;
	
	public ListDatasets (final Category category, DatasetStatusType status, Boolean withCoupling, 
			final String query, long page) {
		this.categoryId = category == null ? null : category.id ();
		this.status = status;
		this.withCoupling = withCoupling;
		this.page = page;
		this.query = query;
	}
	
	public String categoryId () {
		return this.categoryId;
	}
	
	public DatasetStatusType status () {
		return this.status;
	}
	
	public Boolean withCoupling () {
		return this.withCoupling;
	}

	public long getPage() {
		return page;
	}

	public String getQuery () {
		return query;
	}

	@Override
	public String toString() {
		return "ListDatasets [categoryId=" + categoryId + ", status=" + status
				+ ", page=" + page + "]";
	}

}
 