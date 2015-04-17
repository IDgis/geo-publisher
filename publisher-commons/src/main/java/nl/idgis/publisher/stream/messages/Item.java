package nl.idgis.publisher.stream.messages;

import java.io.Serializable;

public final class Item<T> implements Serializable {	

	private static final long serialVersionUID = 3330904797836673942L;
	
	private final T content;
	
	public Item(T content) {
		this.content = content;
	}
	
	public T getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "Item [content=" + content + "]";
	}
}
