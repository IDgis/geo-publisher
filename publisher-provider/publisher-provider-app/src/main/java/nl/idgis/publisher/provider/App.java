package nl.idgis.publisher.provider;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.messages.ConnectFailed;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import nl.idgis.publisher.provider.messages.Connect;
import nl.idgis.publisher.provider.messages.Stop;
import nl.idgis.publisher.tree.Tree;
import nl.idgis.publisher.utils.Boot;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class App extends UntypedActor {

	private final Config config;	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private ActorRef client;
	private Connect connectMessage;

	public App(Config config) {
		this.config = config;		
	}
	
	public static Props props(Config config) {
		return Props.create(App.class, config);
	}

	@Override
	public void preStart() {
		log.info("starting");
		
		Config harvesterConfig = config.getConfig("harvester");
		String harvesterHost = harvesterConfig.getString("host");
		int harvesterPort = harvesterConfig.getInt("port");

		connectMessage = new Connect(new InetSocketAddress(harvesterHost, harvesterPort));
				
		client = getContext().actorOf(Client.props(config, getSelf()), "client");
		client.tell(connectMessage, getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Tree) {
			log.debug(msg.toString());
		} else if (msg instanceof ConnectFailed) {
			log.error(msg.toString());

			ActorSystem system = getContext().system();
			system.scheduler().scheduleOnce(
					Duration.create(5, TimeUnit.SECONDS), client,
					connectMessage, system.dispatcher(), getSelf());
		} else if (msg instanceof ConnectionClosed) {
			client.tell(connectMessage, getSelf());
		} else if (msg instanceof Stop) {
			log.info("stop requested");
			
			getSender().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private static ActorRef app;

	public static void main(String[] args) {
		Boot boot = Boot.init("provider");
		app = boot.startApplication(App.props(boot.getConfig()));
		boot.awaitTermination();
	}
	
	public static void stop(String[] args) {		
		app.tell(new Stop(), ActorRef.noSender());
	}
}