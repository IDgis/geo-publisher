package nl.idgis.publisher.provider;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.provider.messages.ConnectFailed;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import nl.idgis.publisher.provider.messages.Connect;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Provider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef client;
	
	@Override
	public void preStart() {		
		client = getContext().actorOf(Client.props(getSelf(), ClientListener.props(getSelf())), "client");		
		client.tell(new Connect(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.info(msg.toString());
		} else if(msg instanceof ConnectFailed) {
			log.error(msg.toString());
			
			ActorSystem system = getContext().system();
			system.scheduler().scheduleOnce(Duration.create(5, TimeUnit.SECONDS), client, new Connect(), system.dispatcher(), getSelf());
		} else if(msg instanceof ConnectionClosed) {
			client.tell(new Connect(), getSelf());
		} else {
			unhandled(msg);
		}
	}
}