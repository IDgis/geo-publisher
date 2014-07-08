package nl.idgis.publisher.harvester;

import java.net.InetSocketAddress;

import com.typesafe.config.Config;

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
	
	private final int port;
	private final Config sslConfig;
	
	public Server(int port, Config sslConfig) {
		this.port = port;
		this.sslConfig = sslConfig;
	}
	
	public static Props props(int port, Config sslConfig) {
		return Props.create(Server.class, port, sslConfig);
	}
	
	@Override
	public void preStart() {
		final ActorRef tcp = Tcp.get(getContext().system()).manager();
		tcp.tell(TcpMessage.bind(getSelf(), new InetSocketAddress(port), 100), getSelf());
	}

	@Override
	public void onReceive(final Object msg) throws Exception {
		if (msg instanceof CommandFailed) {
			log.error(msg.toString());
			
			getContext().stop(getSelf());
		} else if (msg instanceof Connected) {
			ActorRef listener = getContext().actorOf(ServerListener.props(sslConfig));
			listener.tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
}
