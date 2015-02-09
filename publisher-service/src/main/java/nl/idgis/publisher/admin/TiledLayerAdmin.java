package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class TiledLayerAdmin extends AbstractAdmin {
	
	public TiledLayerAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(TiledLayerAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
	}

	
}
