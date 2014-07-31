package nl.idgis.publisher.database.messages;

import com.mysema.query.types.Order;

public class GetSourceDatasetInfo extends ListQuery {

	private static final long serialVersionUID = -6128241149157834193L;
	
	private final String dataSourceId;
	private final String categoryId;
	
	public GetSourceDatasetInfo (final String dataSource, final String category) {
		
		this(dataSource, category, Order.ASC, null, null);
	}
	
	public GetSourceDatasetInfo (final String dataSource, final String category, 
			final Long offset, final Long limit) {
		
		this(dataSource, category, Order.ASC, offset, limit);
	}
	
	public GetSourceDatasetInfo (final String dataSource, final String category, 
		final Order order, final Long offset, final Long limit) {
		
		super(order, offset, limit);
		
		this.dataSourceId = (dataSource == null ? null : dataSource);
		this.categoryId = (category == null ? null : category);
	}
	
	public String getDataSourceId () {
		return this.dataSourceId;
	}
	
	public String getCategoryId () {
		return this.categoryId;
	}

	@Override
	public String toString() {
		return "GetSourceDatasetInfo [dataSourceId=" + dataSourceId
				+ ", categoryId=" + categoryId + "]";
	}

}
