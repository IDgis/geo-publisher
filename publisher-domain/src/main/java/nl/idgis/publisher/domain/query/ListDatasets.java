package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;

public class ListDatasets implements DomainQuery<Page<Dataset>>{

	private static final long serialVersionUID = -5582949253434171325L;
	
	private final String categoryId;
	
	public ListDatasets (final Category category) {
		this.categoryId = category == null ? null : category.id ();
	}
	
	public String categoryId () {
		return this.categoryId;
	}
}
 