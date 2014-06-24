package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;

import nl.idgis.publisher.protocol.MessageProtocolHandler;
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

	private final ActorRef app;
	private final Props listenerProps;

	public Client(ActorRef app, Props listenerProps) {
		this.app = app;
		this.listenerProps = listenerProps;		
	}
	
	public static Props props(ActorRef app, Props listenerProps) {
		return Props.create(Client.class, app, listenerProps);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Connect) {
			log.debug("connecting");
			
			ActorRef tcp = Tcp.get(getContext().system()).manager();
			tcp.tell(TcpMessage.connect(new InetSocketAddress("localhost", 2014)), getSelf());
		} else if (msg instanceof CommandFailed) {
			log.error(msg.toString());
			app.tell(new ConnectFailed((CommandFailed) msg), getSelf());
		} else if (msg instanceof Connected) {
			log.debug("connected");
			
			ActorRef listener = getContext().actorOf(listenerProps);
			ActorRef handler = getContext().actorOf(MessageProtocolHandler.props(getSender(), listener));
			getSender().tell(TcpMessage.register(handler), getSelf());
			listener.tell(msg, handler);
		} 
	}
}
