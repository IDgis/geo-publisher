package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.SourceDatasetStats;

public class ListSourceDatasets implements DomainQuery<Page<SourceDatasetStats>>{

	private static final long serialVersionUID = -5582949253434171325L;
	
	private final String dataSourceId;
	private final String categoryId;
	private final String searchString;
	private final Boolean withErrors;
	private final Long page;
	
	public ListSourceDatasets (final DataSource dataSource, final Category category, String searchString, final Boolean withErrors, final Long page) {
		this(dataSource == null ? null : dataSource.id (), 
			category == null ? null : category.id (), searchString, withErrors, page);
	}
	
	public ListSourceDatasets (String dataSourceId, String categoryId, String searchString, final Boolean withErrors, final Long page) {
		this.dataSourceId = dataSourceId;
		this.categoryId = categoryId;
		this.searchString = searchString;
		this.page = page;
		this.withErrors = withErrors;
	}
	
	public String dataSourceId () {
		return this.dataSourceId;
	}
	
	public String categoryId () {
		return this.categoryId;
	}
	
	public String getSearchString() {
		return this.searchString;
	}

	public Long getPage () {
		return this.page;
	}

	public Boolean getWithErrors () {
		return withErrors;
	}
}
