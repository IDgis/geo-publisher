package nl.idgis.publisher.domain.response;

import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.web.Entity;

public final class Page<T extends Entity> {
	
	private final List<T> values;
	private final long currentPage;
	private final boolean hasMorePages;
	private final Long pageCount;
	
	Page (final List<T> values, final long currentPage, final boolean hasMorePages, final Long pageCount) {
		this.values = values == null ? Collections.<T>emptyList () : values;
		this.currentPage = currentPage;
		this.hasMorePages = hasMorePages;
		this.pageCount = pageCount;
	}
	
	public List<T> values () {
		return Collections.unmodifiableList (values);
	}
	
	public long currentPage () {
		return currentPage;
	}
	
	public boolean hasMorePages () {
		return hasMorePages;
	}
	
	public boolean hasPageCount () {
		return pageCount != null;
	}
	
	public long pageCount () {
		return hasPageCount () ? pageCount : (currentPage + (hasMorePages ? 1 : 0) + 1);
	}
}
