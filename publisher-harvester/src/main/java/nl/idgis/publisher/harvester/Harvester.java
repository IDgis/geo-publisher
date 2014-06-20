package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.harvester.messages.Harvest;
import scala.concurrent.duration.Duration;

import akka.actor.Props;
import akka.actor.UntypedActor;

public class Harvester extends UntypedActor {
	
	@Override
	public void preStart() {
		Props serverProps = Props.create(Server.class, getSelf());		
		getContext().actorOf(serverProps, "server");
		
		Props initiatorProps = Props.create(Initiator.class, "../server/client*", Duration.create(10, TimeUnit.SECONDS), new Harvest());
		getContext().actorOf(initiatorProps, "initiator");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);		
	}
}
