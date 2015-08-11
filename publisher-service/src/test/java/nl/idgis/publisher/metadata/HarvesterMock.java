package nl.idgis.publisher.metadata;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;

public class HarvesterMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public static Props props() {
		return Props.create(HarvesterMock.class);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetDataSource) {
			log.debug("dataSource requested");
			
			getSender().tell(new NotConnected(), getSelf());
		} else {
			unhandled(msg);
		}
	}

}
