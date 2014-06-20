package nl.idgis.publisher.provider;

import nl.idgis.publisher.protocol.Message;
import nl.idgis.publisher.protocol.metadata.EndOfList;
import nl.idgis.publisher.protocol.metadata.GetList;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Metadata extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetList) {
			log.debug("metadata list requested");
			
			getSender().tell(new Message("harvester", new EndOfList()), getSelf());
		} else {
			unhandled(msg);
		}
	}
}
