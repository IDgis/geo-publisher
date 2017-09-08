package nl.idgis.publisher.domain.query;

import java.util.Optional;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.SourceDatasetStats;

public class ListSourceDatasets implements DomainQuery<Page<SourceDatasetStats>>{
	
	private static final long serialVersionUID = -7951148810454840884L;
	
	private final String dataSourceId;
	private final String categoryId;
	private final String searchString;
	private final Boolean withErrors;
	private final Boolean withNotifications;
	private final Boolean withDataset;
	private final ListSourceDatasetsOrderBy orderBy;
	private final Long page;
	private final Long itemsPerPage;
	
	public ListSourceDatasets (final DataSource dataSource, final Category category, String searchString, final Boolean withErrors, final Boolean withNotifications, final Boolean withDataset, final ListSourceDatasetsOrderBy orderBy, final Long page) {
		this(dataSource, category, searchString, withErrors, withNotifications, withDataset, orderBy, page, null);
	}
	
	public ListSourceDatasets (final DataSource dataSource, final Category category, String searchString, final Boolean withErrors, final Boolean withNotifications, final Boolean withDataset, final ListSourceDatasetsOrderBy orderBy, final Long page, final Long itemsPerPage) {
		this(dataSource == null ? null : dataSource.id (), 
			category == null ? null : category.id (), searchString, withErrors, withNotifications, withDataset, orderBy, page, itemsPerPage);
	}
	
	public ListSourceDatasets (String dataSourceId, String categoryId, String searchString, final Boolean withErrors, final Boolean withNotifications, final Boolean withDataset, final ListSourceDatasetsOrderBy orderBy, final Long page) {
		this(dataSourceId, categoryId, searchString, withErrors, withNotifications, withDataset, orderBy, page, null);
	}
	
	public ListSourceDatasets (String dataSourceId, String categoryId, String searchString, final Boolean withErrors, final Boolean withNotifications, final Boolean withDataset, final ListSourceDatasetsOrderBy orderBy, final Long page, final Long itemsPerPage) {
		this.dataSourceId = dataSourceId;
		this.categoryId = categoryId;
		this.searchString = searchString;
		this.page = page;
		this.withErrors = withErrors;
		this.withNotifications = withNotifications;
		this.withDataset = withDataset;
		this.orderBy = orderBy;
		this.itemsPerPage = itemsPerPage;
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
	
	public Boolean getWithNotifications() {
		return withNotifications;
	}
	
	public Boolean getWithDataset() {
		return withDataset;
	}
	
	public ListSourceDatasetsOrderBy getOrderBy() {
		return orderBy;
	}

	public Optional<Long> itemsPerPage() {
		return Optional.ofNullable(itemsPerPage);
	}
}
