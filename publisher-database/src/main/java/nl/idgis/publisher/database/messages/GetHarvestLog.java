package nl.idgis.publisher.database.messages;

import com.mysema.query.types.Order;

public class GetHarvestLog extends Query {
	
	private static final long serialVersionUID = -8304890543117760729L;
	
	private final String dataSourceId;
	private final Long offset, limit;
	private final Order order;
	
	public GetHarvestLog(String dataSourceId, Order order, Long offset, Long limit) {
		this.dataSourceId = dataSourceId;
		this.offset = offset;
		this.limit = limit;
		this.order = order;
	}

	public String getDataSourceId() {
		return dataSourceId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public Long getOffset() {
		return offset;
	}

	public Long getLimit() {
		return limit;
	}

	public Order getOrder() {
		return order;
	}
	
}
