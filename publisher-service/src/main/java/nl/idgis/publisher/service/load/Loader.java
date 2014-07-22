package nl.idgis.publisher.service.load;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Loader extends UntypedActor {
	
	private ActorRef database, harvester;

	public Loader(ActorRef database, ActorRef harvester) {
		this.database = database;
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(Loader.class, database, harvester);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}
}
