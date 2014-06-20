package nl.idgis.publisher.provider;

import java.util.Collections;
import java.util.Map;

import nl.idgis.publisher.protocol.Close;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.Message;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Connected;

public class Provider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	@Override
	public void preStart() {
		final Map<String, ActorRef> actors = Collections.singletonMap("provider", getSelf());		
		
		final Props clientProps = Props.create(Client.class, getSelf(), actors);		
		getContext().actorOf(clientProps, "client");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Connected) {
			log.debug("connected");
			
			getSender().tell(new Message("harvester", new Hello("My data provider")), getSelf());			
		} else if (msg instanceof Hello) {
			log.debug(msg.toString());
			
			getSender().tell(new Close(), getSender());
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}	
}