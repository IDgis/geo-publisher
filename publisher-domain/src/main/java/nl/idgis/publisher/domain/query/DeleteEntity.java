package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.Identifiable;

public final class DeleteEntity<T extends Identifiable> implements DomainQuery<Boolean> {
	
	private final Class<T> cls;
	private final String id;
	
	public DeleteEntity (final Class<T> cls, final String id) {
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
		return this.cls;
	}
	
	public String id () {
		return this.id;
	}
}
