package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;
import java.util.Map;

import nl.idgis.publisher.protocol.ConnectionHandler;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.CommandFailed;
import akka.io.Tcp.Connected;
import akka.io.Tcp;
import akka.io.TcpMessage;

public class Client extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ActorRef listener;
	private final Map<String, ActorRef> targets;

	public Client(ActorRef listener, Map<String, ActorRef> targets) {
		this.listener = listener;
		this.targets = targets;
	}
	
	@Override
	public void preStart() {
		final ActorRef tcp = Tcp.get(getContext().system()).manager();
		tcp.tell(TcpMessage.connect(new InetSocketAddress("localhost", 2014)), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof CommandFailed) {
			log.error(msg.toString());
			
			getContext().stop(getSelf());
		} else if (msg instanceof Connected) {
			log.debug("connected");			
			
			final Props handlerProps = Props.create(ConnectionHandler.class, getSender(), listener, targets); 
			final ActorRef handler = getContext().actorOf(handlerProps, "handler");

			getSender().tell(TcpMessage.register(handler), getSelf());
			listener.tell(msg, handler);
		}
	}
}
