package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class LayerGroupAdmin extends AbstractAdmin {
	
	public LayerGroupAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(LayerGroupAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
	}

	
}
