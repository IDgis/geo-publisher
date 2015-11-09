package nl.idgis.publisher.loader;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.messages.GetDataSource;

class HarvesterMock extends UntypedActor {
	
	final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	final ActorRef dataSource;
	
	HarvesterMock(ActorRef dataSource) {
		this.dataSource = dataSource;
	}
	
	static Props props(ActorRef dataSource) {
		return Props.create(HarvesterMock.class, dataSource);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("received: " + msg);
		
		if(msg instanceof GetDataSource) {
			getSender().tell(dataSource, getSelf());
		} else {
			unhandled(msg);
		}
	}		
}