package nl.idgis.publisher.database.messages;

import com.mysema.query.types.Order;

public class GetHarvestLog extends ListQuery {

	private static final long serialVersionUID = 518539532860812239L;
	
	private final String dataSourceId;
	
	public GetHarvestLog(String dataSourceId, Order order, Long offset, Long limit) {
		super(order, offset, limit);
		
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	@Override
	public String toString() {
		return "GetHarvestLog [dataSourceId=" + dataSourceId + ", order="
				+ order + ", offset=" + offset + ", limit=" + limit + "]";
	}
	
}
