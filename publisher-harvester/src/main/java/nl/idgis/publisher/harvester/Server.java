package nl.idgis.publisher.harvester;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;

import nl.idgis.publisher.protocol.ConnectionHandler;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.Tcp.CommandFailed;
import akka.io.Tcp.Connected;
import akka.io.TcpMessage;

public class Server extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final ActorRef listener;
	
	private long clientCount = 0;
	
	public Server(ActorRef listener) {
		this.listener = listener;		
	}
	
	public static Props props(ActorRef listener) {
		return Props.create(Server.class, listener);
	}
	
	@Override
	public void preStart() {
		final ActorRef tcp = Tcp.get(getContext().system()).manager();
		tcp.tell(TcpMessage.bind(getSelf(), new InetSocketAddress(2014), 100), getSelf());
	}

	@Override
	public void onReceive(final Object msg) throws Exception {
		if (msg instanceof CommandFailed) {
			log.error(msg.toString());
			
			getContext().stop(getSelf());
		} else if (msg instanceof Connected) {
			log.debug("client connected");
			
			ActorRef clientHandler = getContext().actorOf(ClientHandler.props(), "client" + clientCount++);
			
			Map<String, ActorRef> targets = Collections.singletonMap("harvester", clientHandler);			
			ActorRef connectionHandler = getContext().actorOf(ConnectionHandler.props(getSender(), listener, targets));			
			getSender().tell(TcpMessage.register(connectionHandler), getSelf());
		} else {
			unhandled(msg);
		}
	}
}
