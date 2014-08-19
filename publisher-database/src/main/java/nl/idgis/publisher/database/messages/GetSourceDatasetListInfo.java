package nl.idgis.publisher.database.messages;

import com.mysema.query.types.Order;

public class GetSourceDatasetListInfo extends ListQuery {

	private static final long serialVersionUID = -6128241149157834193L;
	
	private final String dataSourceId;
	private final String categoryId;
	private final String searchString;
	
	public GetSourceDatasetListInfo (final String dataSource, final String category, final String searchString) {
		
		this(dataSource, category, searchString, Order.ASC, null, null);
	}
	
	public GetSourceDatasetListInfo (final String dataSource, final String category, final String searchString,
			final Long offset, final Long limit) {
		
		this(dataSource, category, searchString, Order.ASC, offset, limit);
	}
	
	public GetSourceDatasetListInfo (final String dataSource, final String category, final String searchString,
		final Order order, final Long offset, final Long limit) {
		
		super(order, offset, limit);
		
		this.dataSourceId = (dataSource == null ? null : dataSource);
		this.categoryId = (category == null ? null : category);
		this.searchString = searchString;
	}
	
	public String getDataSourceId () {
		return this.dataSourceId;
	}
	
	public String getCategoryId () {
		return this.categoryId;
	}
	
	public String getSearchString() {
		return this.searchString;
	}

	@Override
	public String toString() {
		return "GetSourceDatasetListInfo [dataSourceId=" + dataSourceId
				+ ", categoryId=" + categoryId + ", searchString="
				+ searchString + "]";
	}	

}
