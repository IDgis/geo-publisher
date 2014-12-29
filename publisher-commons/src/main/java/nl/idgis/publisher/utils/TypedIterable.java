package nl.idgis.publisher.utils;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

public class TypedIterable<T> implements Iterable<T>, Serializable {

	private static final long serialVersionUID = -6088987471942777255L;
	
	private final Class<T> contentType;
	protected final Iterable<T> content;
	
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
	
	public Collection<T> asCollection() {
		if(content instanceof Collection) {
			return (Collection<T>)content;
		}
		
		return new AbstractCollection<T>() {
			
			private Integer size;

			@Override
			public Iterator<T> iterator() {
				return iterator();
			}

			@Override
			public int size() {				
				if(size == null) {
					int size = 0;
					
					for(Iterator<T> i = iterator(); i.hasNext();) {
						size++;
					}
					
					this.size = size;
				}
				
				return size;
			}
			
		};
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result
				+ ((contentType == null) ? 0 : contentType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TypedIterable<?> other = (TypedIterable<?>) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TypedIterable [contentType=" + contentType + ", content="
				+ content + "]";
	}

}
