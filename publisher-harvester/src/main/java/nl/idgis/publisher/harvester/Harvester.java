package nl.idgis.publisher.harvester;

import java.util.Collections;
import java.util.Map;

import nl.idgis.publisher.protocol.Hello;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Harvester extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	@Override
	public void preStart() {
		Map<String, ActorRef> actors = Collections.singletonMap("harvester", getSelf());
		
		Props serverProps = Props.create(Server.class, getSelf(), actors);		
		getContext().actorOf(serverProps, "server");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
			
			Props handlerProps = Props.create(ClientHandler.class, getSender());
			getContext().actorOf(handlerProps);
		} else {
			unhandled(msg);
		}
	}
}
