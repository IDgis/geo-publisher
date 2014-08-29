package nl.idgis.publisher.domain.query;

import java.sql.Timestamp;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Notification;

public final class ListActiveNotifications implements DomainQuery<Page<Notification>> {
	private static final long serialVersionUID = -7622683331145599216L;
	
	private final boolean includeRejected;
	private final Timestamp since;
	private final Long page;
	private final Long limit;
	
	public ListActiveNotifications (final boolean includeRejected, final Long page) {
		this (includeRejected, null, page, null);
	}
	
	public ListActiveNotifications (final boolean includeRejected, final Timestamp since, final Long page, final Long limit) {
		this.includeRejected = includeRejected;
		this.since = since;
		this.page = page;
		this.limit = limit;
	}

	public boolean isIncludeRejected () {
		return includeRejected;
	}

	public Timestamp getSince () {
		return since;
	}

	public Long getPage () {
		return page;
	}

	public Long getLimit () {
		return limit;
	}
}
