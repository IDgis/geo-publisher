package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.utils.Boot;
import nl.idgis.publisher.utils.ConfigUtils;
import nl.idgis.publisher.utils.Initiator;

import scala.concurrent.duration.Duration;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Harvester extends UntypedActor {	
	
	private final Config config;
	private final LoggingAdapter log;

	public Harvester(Config config) {
		this.config = config;
		
		log = Logging.getLogger(getContext().system(), this);
	}
	
	public static Props props(Config config) {
		return Props.create(Harvester.class, config);
	}

	@Override
	public void preStart() {
		
		final int port = config.getInt("port");
		final Config sslConfig = ConfigUtils.getOptionalConfig(config, "ssl");		
		getContext().actorOf(Server.props(port, sslConfig), "server");

		getContext().actorOf(
				Initiator.props("../server/*/harvester",
						Duration.create(10, TimeUnit.SECONDS), new Harvest()),
				"initiator");
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
		boot.startApplication(Harvester.props(boot.getConfig()));
	}
}
