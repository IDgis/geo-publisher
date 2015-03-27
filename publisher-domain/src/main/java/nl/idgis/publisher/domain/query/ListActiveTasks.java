package nl.idgis.publisher.domain.query;

import java.sql.Timestamp;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.ActiveTask;

public final class ListActiveTasks implements DomainQuery<Page<ActiveTask>> {
	
	private static final long serialVersionUID = -2441947297680035377L;

	private final Timestamp since;
	private final Long page;
	private final Long limit;
	
	public ListActiveTasks (final Long page) {
		this (null, page, null);
	}
	
	public ListActiveTasks (final Timestamp since, final Long page, final Long limit) {
		this.since = since;
		this.page = page;
		this.limit = limit;
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
