package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;

import com.typesafe.config.Config;

import nl.idgis.publisher.provider.messages.ConnectFailed;
import nl.idgis.publisher.provider.messages.Connect;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.io.Tcp;
import akka.io.Tcp.CommandFailed;
import akka.io.Tcp.Connected;
import akka.io.TcpMessage;

public class Client extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Config config;
	private final ActorRef app, monitor;	
	
	public Client(Config config, ActorRef app, ActorRef monitor) {
		this.config = config;
		this.app = app;
		this.monitor = monitor;
	}
	
	public static Props props(Config config, ActorRef app, ActorRef monitor) {
		return Props.create(Client.class, config, app, monitor);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Connect) {
			InetSocketAddress address = ((Connect) msg).getAddress();
			
			log.debug("connecting to " + address);
			
			ActorRef tcp = Tcp.get(getContext().system()).manager();
			tcp.tell(TcpMessage.connect(address), getSelf());
		} else if (msg instanceof CommandFailed) {
			log.error(msg.toString());
			app.tell(new ConnectFailed((CommandFailed) msg), getSelf());
		} else if (msg instanceof Connected) {
			log.debug("connected");
			
			getContext().actorOf(ClientListener.props(getSender(), config, app, monitor));			
		} 
	}
}
