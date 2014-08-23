package nl.idgis.publisher.utils;

import java.io.Serializable;
import java.util.Iterator;

public class TypedIterable<T> implements Iterable<T>, Serializable {

	private static final long serialVersionUID = -6088987471942777255L;
	
	private final Class<T> contentType;
	private final Iterable<T> content;
	
	public TypedIterable(Class<T> contentType, Iterable<T> content) {
		this.contentType = contentType;
		this.content = content;
	}
	
	public boolean contains(Class<?> contentType) {
		return contentType.isAssignableFrom(this.contentType);
	}
	
	@SuppressWarnings("unchecked")
	public <U> TypedIterable<U> cast(Class<U> contentType) {
		if(contains(contentType)) {
			return (TypedIterable<U>)this;
		} else {
			throw new ClassCastException();
		}
	}

	@Override
	public Iterator<T> iterator() {
		return content.iterator();
	}

	@Override
	public String toString() {
		return "TypedIterable [contentType=" + contentType + ", content="
				+ content + "]";
	}

}
