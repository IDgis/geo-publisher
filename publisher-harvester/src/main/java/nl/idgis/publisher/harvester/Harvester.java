package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.harvester.messages.Harvest;

import scala.concurrent.duration.Duration;

import akka.actor.UntypedActor;

public class Harvester extends UntypedActor {

	@Override
	public void preStart() {		
		getContext().actorOf(Server.props(ServerListener.props()), "server");

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
