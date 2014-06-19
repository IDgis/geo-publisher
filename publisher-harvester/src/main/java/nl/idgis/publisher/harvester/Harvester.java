package nl.idgis.publisher.harvester;

import akka.actor.ActorSystem;

public class Harvester {
	
	public static void main(String[] args) {
		final ActorSystem actorSystem = ActorSystem.create("harvester");
		
		actorSystem.log().debug("started");
	}
}
