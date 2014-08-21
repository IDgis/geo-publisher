package nl.idgis.publisher.service;

import nl.idgis.publisher.database.messages.ServiceJobInfo;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Service extends UntypedActor {
	
	private final ActorRef database;
	
	public Service(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(Service.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ServiceJobInfo) {
			
		} else {		
			unhandled(msg);
		}
	}

}
