package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

import static nl.idgis.publisher.utils.MathUtils.toPrettySize;

public class TransferedTotal implements Serializable {

	private static final long serialVersionUID = -6337192275023750372L;
	
	private final long received, sent;
	
	public TransferedTotal(long received, long sent) {
		this.received = received;
		this.sent = sent;
	}

	public long getReceived() {
		return received;
	}

	public long getSent() {
		return sent;
	}

	@Override
	public String toString() {
		return "TransferedTotal [received=" + toPrettySize(received) + ", sent=" + toPrettySize(sent) + "]";
	}
}
