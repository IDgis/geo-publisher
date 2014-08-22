package nl.idgis.publisher.service;

import nl.idgis.publisher.database.messages.ServiceJobInfo;
import nl.idgis.publisher.service.rest.ServiceRest;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class Service extends UntypedActor {
	
	private final ActorRef database;
	private final ServiceRest rest;

	public Service(ActorRef database, String serviceLocation, String user, String password) throws Exception {
		this.database = database;
		
		rest = new ServiceRest(serviceLocation, user, password);
	}
	
	public static Props props(ActorRef database, String serviceLocation, String user, String password) {
		return Props.create(Service.class, database, serviceLocation, user, password);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ServiceJobInfo) {
			handleServiceJob((ServiceJobInfo)msg);
		} else {		
			unhandled(msg);
		}
	}
	
	private void handleServiceJob(ServiceJobInfo msg) {
		
	}	
}
