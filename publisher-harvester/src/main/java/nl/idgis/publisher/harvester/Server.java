package nl.idgis.publisher.harvester;

import java.util.Map;

import nl.idgis.publisher.protocol.ConnectionHandler;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.CommandFailed;
import akka.io.Tcp.Connected;
import akka.io.TcpMessage;

public class Server extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Map<String, ActorRef> targets;
	
	public Server(Map<String, ActorRef> targets) {
		this.targets = targets;
	}

	@Override
	public void onReceive(final Object msg) throws Exception {
		if (msg instanceof CommandFailed) {
			getContext().stop(getSelf());
		} else if (msg instanceof Connected) {
			final ActorRef connection = getSender();

			log.debug("client connected");

			final ActorRef handler = getContext().actorOf(
					Props.create(ConnectionHandler.class, connection, targets),
					"handler");
			connection.tell(TcpMessage.register(handler), getSelf());
		} else {
			unhandled(msg);
		}
	}
}
