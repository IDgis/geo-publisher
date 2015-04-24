package nl.idgis.publisher.stream.messages;

import java.io.Serializable;
import java.util.Optional;

public class NextItem implements Serializable {

	private static final long serialVersionUID = 3025651954969003746L;
	
	private final Long seq;
	
	public NextItem() {
		seq = null;
	}
	
	public NextItem(long seq) {
		this.seq = seq;
	}
	
	public Optional<Long> getSequenceNumber() {
		return Optional.ofNullable(seq);
	}

	@Override
	public String toString() {
		return "NextItem [seq=" + seq + "]";
	}
	
}
