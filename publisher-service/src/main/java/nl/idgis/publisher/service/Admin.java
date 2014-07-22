package nl.idgis.publisher.service;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Admin extends UntypedActor {
	
	private final ActorRef database, harvester;
	
	public Admin(ActorRef database, ActorRef harvester) {
		this.database = database;
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(Admin.class, database, harvester);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}
}
