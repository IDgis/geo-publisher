package nl.idgis.publisher.harvester.sources;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;

public class ProviderDataset extends UntypedActor {
	
	private final ActorRef sink;

	public ProviderDataset(ActorRef sink) {
		this.sink = sink;
	}
	
	public static Props props(ActorRef sink) {
		return Props.create(ProviderDataset.class, sink);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().watch(sink);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Terminated) {
			getContext().stop(getSelf());
		} else {
			sink.tell(msg, getSelf());
		}
	}
	
	
}
