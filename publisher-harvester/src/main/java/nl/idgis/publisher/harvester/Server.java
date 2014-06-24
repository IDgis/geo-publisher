package nl.idgis.publisher.harvester;

import java.net.InetSocketAddress;

import nl.idgis.publisher.protocol.MessageProtocolHandler;

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
	
	private long clientCount = 0;

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Props listenerProps;
	
	public Server(Props listenerProps) {
		this.listenerProps = listenerProps;
	}
	
	public static Props props(Props listenerProps) {
		return Props.create(Server.class, listenerProps);
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
			
			ActorRef listener = getContext().actorOf(listenerProps, "client" + clientCount);
			ActorRef handler = getContext().actorOf(MessageProtocolHandler.props(getSender(), listener), "handler" + clientCount);
			getSender().tell(TcpMessage.register(handler), getSelf());
			listener.tell(msg, handler);
			
			clientCount++;
		} else {
			unhandled(msg);
		}
	}
}
