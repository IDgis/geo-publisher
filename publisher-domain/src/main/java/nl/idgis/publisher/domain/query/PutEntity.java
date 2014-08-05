package nl.idgis.publisher.domain.query;


import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.web.Identifiable;

public final class PutEntity<T extends Identifiable> implements DomainQuery<Response<?>> {

	private static final long serialVersionUID = 3056189862216804454L;
	
	private final T value;
	
	public PutEntity (final T value) {
		if (value == null) {
			throw new NullPointerException ("value cannot be null");
		}
		
		this.value = value;
	}
	
	public T value () {
		return value;
	}
}
