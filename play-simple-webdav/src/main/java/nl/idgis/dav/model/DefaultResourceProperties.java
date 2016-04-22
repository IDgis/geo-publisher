package nl.idgis.dav.model;

import java.util.Date;
import java.util.Optional;

public class DefaultResourceProperties implements ResourceProperties {
	
	private final boolean collection;
	
	private final Date lastModified;
	
	public DefaultResourceProperties(boolean collection) {
		this(collection, null);
	}
	
	public DefaultResourceProperties(boolean collection, Date lastModified) {
		this.collection = collection;
		this.lastModified = lastModified;
	}

	@Override
	public boolean collection() {
		return collection;
	}
	
	@Override
	public Optional<Date> lastModified() {
		return Optional.ofNullable(lastModified);
	}
}