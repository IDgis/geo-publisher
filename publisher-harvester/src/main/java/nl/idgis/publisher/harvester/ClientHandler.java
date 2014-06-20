package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.protocol.Close;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.Message;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;

public class ClientHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final FiniteDuration interval = Duration.create(1, TimeUnit.SECONDS);
	private final ActorRef connection;
	
	public ClientHandler(ActorRef connection) {
		this.connection = connection;
	}
	
	@Override
	public void preStart() {
		connection.tell(new Message("provider", new Hello("My data harvester")), getSelf());
		
		ActorSystem system = getContext().system();
		system.scheduler().scheduleOnce(interval, connection, new Close(), system.dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
}
