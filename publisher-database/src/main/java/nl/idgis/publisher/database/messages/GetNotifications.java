package nl.idgis.publisher.database.messages;

import java.sql.Timestamp;

import com.mysema.query.types.Order;

public final class GetNotifications extends ListQuery {
	private static final long serialVersionUID = 7723895173712842178L;
	
	private final boolean includeRejected;
	private final Timestamp since;
	
	public GetNotifications (
			final Order order, 
			final Long offset, 
			final Long limit, 
			final boolean includeRejected, 
			final Timestamp since) {
		
		super (order, offset, limit);
		
		this.includeRejected = includeRejected;
		this.since = since;
	}

	public boolean isIncludeRejected () {
		return includeRejected;
	}

	public Timestamp getSince () {
		return since;
	}
}
