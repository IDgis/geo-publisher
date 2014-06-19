package nl.idgis.publisher.provider;

import akka.actor.ActorSystem;

public class Provider {
	
	public static void main(String[] args) {
		final ActorSystem actorSystem = ActorSystem.create("provider");
		
		actorSystem.log().debug("started");
	}
}