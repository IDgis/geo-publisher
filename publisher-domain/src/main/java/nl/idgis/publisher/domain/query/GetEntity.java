package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.Entity;

public final class GetEntity<T extends Entity> implements DomainQuery<T> {

	private static final long serialVersionUID = -1525247897394477556L;
	
	private final Class<T> cls;
	private final String id;
	
	public GetEntity (final Class<T> cls, final String id) {
		if (cls == null) {
			throw new NullPointerException ("cls cannot be null");
		}
		if (id == null) {
			throw new NullPointerException ("id cannot be null");
		}
		
		this.cls = cls;
		this.id = id;
	}
	
	public Class<T> cls () {
		return cls;
	}
	
	public String id () {
		return id;
	}
}
