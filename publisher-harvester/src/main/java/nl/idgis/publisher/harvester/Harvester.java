package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.utils.Initiator;
import scala.concurrent.duration.Duration;
import akka.actor.UntypedActor;

public class Harvester extends UntypedActor {

	@Override
	public void preStart() {
		Config conf = ConfigFactory.load();
		
		Config harvesterConf = conf.getConfig("publisher.harvester");		
		
		getContext().actorOf(Server.props(ServerListener.props(harvesterConf), harvesterConf.getInt("port")), "server");

		getContext().actorOf(
				Initiator.props("../server/client*/harvester",
						Duration.create(10, TimeUnit.SECONDS), new Harvest()),
				"initiator");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);		
	}
}
