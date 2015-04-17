package nl.idgis.publisher.stream.messages;

import java.io.Serializable;

public final class Item<T> implements Serializable {	

	private static final long serialVersionUID = 4918912562028335328L;

	private final long seq;
	
	private final T content;
	
	public Item(long seq, T content) {
		this.seq = seq;
		this.content = content;
	}
	
	public long getSequenceNumber() {
		return seq;
	}
	
	public T getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "Item [seq=" + seq + ", content=" + content + "]";
	}
	
}
