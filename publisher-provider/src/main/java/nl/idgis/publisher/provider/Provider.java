package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import scala.concurrent.duration.Duration;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.provider.messages.ConnectFailed;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import nl.idgis.publisher.provider.messages.Connect;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Provider extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);

	private ActorRef client;
	private Connect connectMessage;

	@Override
	public void preStart() {
		Config conf = ConfigFactory.load();
		
		Config providerConfig = conf.getConfig("publisher.provider");		
		
		Config harvesterConfig = providerConfig.getConfig("harvester");
		String harvesterHost = harvesterConfig.getString("host");
		int harvesterPort = harvesterConfig.getInt("port");
		
		connectMessage = new Connect(new InetSocketAddress(harvesterHost, harvesterPort));
		
		client = getContext().actorOf(Client.props(getSelf(), ClientListener.props(providerConfig, getSelf())), "client");		
		client.tell(connectMessage, getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Hello) {
			log.info(msg.toString());
		} else if (msg instanceof ConnectFailed) {
			log.error(msg.toString());

			ActorSystem system = getContext().system();
			system.scheduler().scheduleOnce(
					Duration.create(5, TimeUnit.SECONDS), client,
					connectMessage, system.dispatcher(), getSelf());
		} else if (msg instanceof ConnectionClosed) {
			client.tell(connectMessage, getSelf());
		} else {
			unhandled(msg);
		}
	}
}