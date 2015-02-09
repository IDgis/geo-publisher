package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class ServiceAdmin extends AbstractAdmin {
	
	public ServiceAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(ServiceAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
	}

	
}
