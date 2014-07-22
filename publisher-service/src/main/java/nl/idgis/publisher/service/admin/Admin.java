package nl.idgis.publisher.service.admin;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Admin extends UntypedActor {
	
	private final ActorRef database, harvester, loader;
	
	public Admin(ActorRef database, ActorRef harvester, ActorRef loader) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader) {
		return Props.create(Admin.class, database, harvester, loader);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}
}
