package nl.idgis.publisher.domain.response;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.idgis.publisher.domain.web.Entity;

public final class Page<T extends Entity> implements Serializable {
	
	private static final long serialVersionUID = 6206734086529244167L;
	
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
	
	public static class Builder<A extends Entity> {
		private final List<A> list = new ArrayList<A> ();
		private long currentPage = 0;
		private boolean hasMorePages = false;
		private Long pageCount = null;
		
		public long getCurrentPage() {
			return currentPage;
		}
		
		public Builder<A> setCurrentPage(long currentPage) {
			this.currentPage = currentPage;
			return this;
		}
		
		public boolean isHasMorePages() {
			return hasMorePages;
		}
		
		public Builder<A> setHasMorePages(boolean hasMorePages) {
			this.hasMorePages = hasMorePages;
			return this;
		}
		
		public Long getPageCount() {
			return pageCount;
		}
		
		public Builder<A> setPageCount(Long pageCount) {
			this.pageCount = pageCount;
			return this;
		}
		
		public void add (final A value) {
			list.add (value);
		}
		
		public Page<A> build () {
			return new Page<A> (list, currentPage, hasMorePages, pageCount);
		}
	}
}
