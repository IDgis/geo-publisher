package nl.idgis.publisher.stream.messages;

import java.io.Serializable;

import akka.actor.ActorRef;

public class AggregateStream implements Serializable {

	private static final long serialVersionUID = 3562330115934966471L;
	
	private final ActorRef streamProvider;
	private final Start start;
	
	public AggregateStream(ActorRef streamProvider, Start start) {
		this.streamProvider = streamProvider;
		this.start = start;
	}

	public ActorRef getStreamProvider() {
		return streamProvider;
	}

	public Start getStart() {
		return start;
	}

	@Override
	public String toString() {
		return "AggregateStream [streamProvider=" + streamProvider + ", start="
				+ start + "]";
	}
}
