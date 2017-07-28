package nl.idgis.publisher.harvester.server;

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

import nl.idgis.publisher.utils.UniqueNameGenerator;

public class Server extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final String harvesterName;
	
	private final ActorRef harvester;
	
	private final int port;
	
	private final Config harvesterConfig;
	
	public Server(String harvesterName, ActorRef harvester, int port, Config harvesterConfig) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.port = port;
		this.harvesterConfig = harvesterConfig;
	}
	
	public static Props props(String harvesterName, ActorRef harvester, int port, Config harvesterConfig) {
		return Props.create(Server.class, harvesterName, harvester, port, harvesterConfig);
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
			ActorRef listener = getContext().actorOf(
					ServerListener.props(harvesterName, harvester, harvesterConfig),
					nameGenerator.getName(ServerListener.class));			
			listener.tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
}
