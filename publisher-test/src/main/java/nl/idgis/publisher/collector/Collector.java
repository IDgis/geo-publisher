package nl.idgis.publisher.collector;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.collector.messages.GetMessage;

public class Collector extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private Object received;
	
	private ActorRef sender;
	
	public static Props props() {
		return Props.create(Collector.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception { 
		if(msg instanceof GetMessage) {
			log.debug("message requested");
			
			sender = getSender();
		} else {
			log.debug("message received");
			
			received = msg;
		}
		
		if(sender != null && received != null) {
			sender.tell(received, getSelf());
			getContext().stop(getSelf());
			
			log.debug("message delivered");
		}
	}

}
