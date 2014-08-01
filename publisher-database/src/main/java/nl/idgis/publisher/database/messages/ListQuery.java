package nl.idgis.publisher.database.messages;

import com.mysema.query.types.Order;

public abstract class ListQuery extends Query {

	private static final long serialVersionUID = 486819214732070272L;
	
	protected final Order order;
	protected final Long offset, limit;	
	
	public ListQuery(Order order, Long offset, Long limit) {
		this.offset = offset;
		this.limit = limit;
		this.order = order;
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
