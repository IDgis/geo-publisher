package nl.idgis.publisher.utils;

import java.util.List;
import java.util.ListIterator;

public class TypedList<T> extends TypedIterable<T> {

	private static final long serialVersionUID = -4796978645944165648L;

	public TypedList(Class<T> contentType, List<T> content) {
		super(contentType, content);
	}

	public ListIterator<T> listIterator() {
		return ((List<T>)content).listIterator();
	}
}
