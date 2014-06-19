package nl.idgis.publisher.provider;

import java.util.Map;

import nl.idgis.publisher.protocol.ConnectionHandler;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.io.Tcp.CommandFailed;
import akka.io.Tcp.Connected;
import akka.io.TcpMessage;

public class Client extends UntypedActor {

	private final ActorRef listener;
	private final Map<String, ActorRef> targets;

	public Client(ActorRef listener, Map<String, ActorRef> targets) {
		this.listener = listener;
		this.targets = targets;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof CommandFailed) {
			getContext().stop(getSelf());
		} else if (msg instanceof Connected) {
			final ActorRef connection = getSender();
			final ActorRef handler = getContext().actorOf(
					Props.create(ConnectionHandler.class, connection, targets),
					"handler");

			connection.tell(TcpMessage.register(handler), getSelf());
			listener.tell(msg, handler);
		}
	}
}
