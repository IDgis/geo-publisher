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
	private final Long page;
	
	public ListSourceDatasets (final DataSource dataSource, final Category category) {
		this(dataSource, category, null, null);
	}
	
	public ListSourceDatasets (final DataSource dataSource, final Category category, String searchString) {
		this(dataSource, category, searchString, null);
	}
	
	public ListSourceDatasets (final DataSource dataSource, final Category category, final Long page) {
		this(dataSource, category, null, page);
	}
	
	public ListSourceDatasets (final DataSource dataSource, final Category category, String searchString, final Long page) {
		this(dataSource == null ? null : dataSource.id (), 
			category == null ? null : category.id (), searchString, page);
	}
	
	public ListSourceDatasets (String dataSourceId, String categoryId) {
		this(dataSourceId, categoryId, null, null);
	}
	
	public ListSourceDatasets (String dataSourceId, String categoryId, String searchString) {
		this(dataSourceId, categoryId, searchString, null);
	}
	
	public ListSourceDatasets (String dataSourceId, String categoryId, final Long page) {
		this(dataSourceId, categoryId, null, page);
	}
	
	public ListSourceDatasets (String dataSourceId, String categoryId, String searchString, final Long page) {
		this.dataSourceId = dataSourceId;
		this.categoryId = categoryId;
		this.searchString = searchString;
		this.page = page;
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
}
