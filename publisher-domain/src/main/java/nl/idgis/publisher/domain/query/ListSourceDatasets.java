package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.SourceDatasetStats;

public class ListSourceDatasets implements DomainQuery<Page<SourceDatasetStats>>{

	private static final long serialVersionUID = -5582949253434171325L;
	
	private final String dataSourceId;
	private final String categoryId;
	
	public ListSourceDatasets (final DataSource dataSource, final Category category) {
		this.dataSourceId = dataSource == null ? null : dataSource.id ();
		this.categoryId = category == null ? null : category.id ();
	}
	
	public String dataSourceId () {
		return this.dataSourceId;
	}
	
	public String categoryId () {
		return this.categoryId;
	}
}
