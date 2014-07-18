package nl.idgis.publisher.domain.web;

import java.io.Serializable;

public abstract class Entity implements Serializable {

	private static final long serialVersionUID = 6129507906003448248L;

	@Override
	public String toString () {
		return "[" + getClass ().getName () + "]";
	}
}
