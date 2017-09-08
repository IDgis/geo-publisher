package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.DatasetStatusType;

public class ListDatasets implements DomainQuery<Page<Dataset>>{
	
	private static final long serialVersionUID = -5179627828015295011L;

	private final String categoryId;
	
	private final DatasetStatusType status;
	
	private final Boolean withLayer;
	
	private final long page;
	
	private final String query;
	
	public ListDatasets (final Category category, DatasetStatusType status, Boolean withLayer, 
			final String query, long page) {
		this.categoryId = category == null ? null : category.id ();
		this.status = status;
		this.withLayer = withLayer;
		this.page = page;
		this.query = query;
	}
	
	public String categoryId () {
		return this.categoryId;
	}
	
	public DatasetStatusType status () {
		return this.status;
	}
	
	public Boolean withLayer () {
		return this.withLayer;
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
				+ ", withLayer" + withLayer + ", page=" + page + "]";
	}

}
 