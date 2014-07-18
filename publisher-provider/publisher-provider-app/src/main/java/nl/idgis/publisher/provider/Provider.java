package nl.idgis.publisher.provider;

import nl.idgis.publisher.protocol.messages.Hello;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Provider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public static Props props() {
		return Props.create(Provider.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
		} else {
			unhandled(msg);
		}
	}

}
