package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonGetter;

public abstract class Identifiable extends Entity {

	private static final long serialVersionUID = 8363301811170195861L;
	
	private final String id;

	public Identifiable (final String id) {
		this.id = id;
	}
	
	@JsonGetter
	public String id () {
		return this.id;
	}

	@Override
	public String toString () {
		return "[" + getClass ().getName () + ": " + id () + "]";
	}
	
	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals (final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Identifiable other = (Identifiable) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
}
