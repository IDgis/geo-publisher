package nl.idgis.publisher.harvester;

import java.net.InetSocketAddress;
import java.util.Collections;

import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.Message;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;

public class Harvester extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
			
			getSender().tell(new Message("provider", new Hello("My data harvester")), getSelf());
		} else {
			unhandled(msg);
		}
	}

	public static void main(String[] args) {
		final ActorSystem actorSystem = ActorSystem.create("harvester");

		final ActorRef harvester = actorSystem.actorOf(
				Props.create(Harvester.class), "harvester");
		final ActorRef server = actorSystem.actorOf(
				Props.create(Server.class,
						Collections.singletonMap("harvester", harvester)),
				"server");

		final ActorRef tcp = Tcp.get(actorSystem).manager();
		tcp.tell(TcpMessage.bind(server, new InetSocketAddress(2014), 100),
				server);

		actorSystem.log().debug("started");
	}
}
