package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.monitor.messages.GetTree;
import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.provider.messages.ConnectFailed;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import nl.idgis.publisher.provider.messages.Connect;
import nl.idgis.publisher.utils.Boot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Provider extends UntypedActor {

	private final Config config;
	private final ActorRef monitor;
	private final LoggingAdapter log;	

	private ActorRef client;
	private Connect connectMessage;

	public Provider(Config config, ActorRef monitor) {
		log = Logging.getLogger(getContext().system(), this);

		this.config = config;
		this.monitor = monitor;
	}
	
	public static Props props(Config config, ActorRef monitor) {
		return Props.create(Provider.class, config, monitor);
	}

	@Override
	public void preStart() {
		Config harvesterConfig = config.getConfig("harvester");
		String harvesterHost = harvesterConfig.getString("host");
		int harvesterPort = harvesterConfig.getInt("port");

		connectMessage = new Connect(new InetSocketAddress(harvesterHost, harvesterPort));
				
		client = getContext().actorOf(Client.props(config, getSelf(), monitor), "client");
		client.tell(connectMessage, getSelf());
		
		ActorSystem system = getContext().system();
		system.scheduler().schedule(Duration.Zero(), Duration.create(10, TimeUnit.SECONDS), 
				monitor, new GetTree(), system.dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Hello) {
			log.info(msg.toString());
		} else if (msg instanceof Tree) {
			log.debug(msg.toString());
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

	public static void main(String[] args) {
		Boot boot = Boot.init("provider");
		boot.startPublisher(Provider.props(boot.getConfig(), boot.getMonitor()));
	}
}