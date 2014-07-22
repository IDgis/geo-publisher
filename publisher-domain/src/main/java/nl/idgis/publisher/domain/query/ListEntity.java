package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Entity;

public class ListEntity<T extends Entity> implements DomainQuery<Page<T>> {
	
	private final Class<T> cls;
	private final long page;
	
	public ListEntity (final Class<T> cls, final long page) {
		if (cls == null) {
			throw new NullPointerException ("cls cannot be null");
		}
		
		this.cls = cls;
		this.page = page;
	}

	public Class<T> cls () {
		return cls;
	}
	
	public long page () {
		return page;
	}
}
