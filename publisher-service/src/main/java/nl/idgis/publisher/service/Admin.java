package nl.idgis.publisher.service;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Admin extends UntypedActor {
	
	private final ActorRef database;
	
	public Admin(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(Admin.class, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}
}
