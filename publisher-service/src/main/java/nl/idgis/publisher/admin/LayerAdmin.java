package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class LayerAdmin extends AbstractAdmin {
	
	public LayerAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(LayerAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
	}

	
}
