package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.monitor.messages.GetTree;
import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.utils.Boot;
import nl.idgis.publisher.utils.Initiator;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Harvester extends UntypedActor {	
	
	private final Config config;
	private final ActorRef monitor;
	private final LoggingAdapter log;

	public Harvester(Config config, ActorRef monitor) {
		this.config = config;
		this.monitor = monitor;
		
		log = Logging.getLogger(getContext().system(), this);
	}
	
	public static Props props(Config config, ActorRef monitor) {
		return Props.create(Harvester.class, config, monitor);
	}

	@Override
	public void preStart() {
		
		getContext().actorOf(Server.props(config.getInt("port")), "server");

		getContext().actorOf(
				Initiator.props("../server/*/harvester",
						Duration.create(10, TimeUnit.SECONDS), new Harvest()),
				"initiator");
		
		ActorSystem system = getContext().system();
		system.scheduler().schedule(Duration.Zero(), Duration.create(10, TimeUnit.SECONDS), 
				monitor, new GetTree(), system.dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Tree) {
			log.debug(msg.toString());
		} else {
			unhandled(msg);
		}
	}
	
	public static void main(String[] args) {
		Boot boot = Boot.init("harvester");
		boot.startPublisher(Harvester.props(boot.getConfig(), boot.getMonitor()));
	}
}
