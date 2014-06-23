package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;

import nl.idgis.publisher.protocol.MessageProtocolHandler;
import nl.idgis.publisher.provider.messages.CreateConnection;

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

	private final ActorRef listener;

	public Client(ActorRef listener) {
		this.listener = listener;		
	}
	
	public static Props props(ActorRef listener) {
		return Props.create(Client.class, listener);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof CreateConnection) {
			log.debug("connecting");
			
			ActorRef tcp = Tcp.get(getContext().system()).manager();
			tcp.tell(TcpMessage.connect(new InetSocketAddress("localhost", 2014)), getSelf());
		} else if (msg instanceof CommandFailed) {
			log.error(msg.toString());
			
			getContext().stop(getSelf());
		} else if (msg instanceof Connected) {
			log.debug("connected");
			
			final ActorRef handler = getContext().actorOf(MessageProtocolHandler.props(getSender(), listener), "handler");
			getSender().tell(TcpMessage.register(handler), getSelf());
			listener.tell(msg, handler);
		} 
	}
}
