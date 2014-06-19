package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;
import java.util.Collections;

import nl.idgis.publisher.protocol.Close;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.Message;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.Tcp.Connected;
import akka.io.TcpMessage;

public class Provider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Connected) {
			getSender().tell(new Message("harvester", new Hello("My data provider")), getSelf());			
		} else if (msg instanceof Hello) {
			log.debug(msg.toString());
			getSender().tell(new Close(), getSender());
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}

	public static void main(String[] args) {
		final ActorSystem actorSystem = ActorSystem.create("provider");

		final ActorRef provider = actorSystem.actorOf(
				Props.create(Provider.class), "provider");
		final ActorRef client = actorSystem.actorOf(
				Props.create(Client.class, provider,
						Collections.singletonMap("provider", provider)),
				"client");

		final ActorRef tcp = Tcp.get(actorSystem).manager();
		tcp.tell(TcpMessage.connect(new InetSocketAddress("localhost", 2014)),
				client);

		actorSystem.log().debug("started");
	}
}