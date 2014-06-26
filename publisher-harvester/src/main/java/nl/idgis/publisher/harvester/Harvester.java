package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.utils.Boot;
import nl.idgis.publisher.utils.Initiator;
import scala.concurrent.duration.Duration;
import akka.actor.UntypedActor;

public class Harvester extends UntypedActor {
	
	private final Config config;

	public Harvester(Config config) {
		this.config = config;
	}

	@Override
	public void preStart() {
		
		getContext().actorOf(Server.props(ServerListener.props(config), config.getInt("port")), "server");

		getContext().actorOf(
				Initiator.props("../server/client*/harvester",
						Duration.create(10, TimeUnit.SECONDS), new Harvest()),
				"initiator");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);		
	}
	
	public static void main(String[] args) {
		Boot.startPublisher("harvester", Harvester.class);
	}
}
