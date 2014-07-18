package nl.idgis.publisher.protocol.stream;

import akka.actor.ActorRef;

public class AggregateStream {

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
}
