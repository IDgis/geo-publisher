package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class SourceDatasetAdmin extends AbstractAdmin {

	public SourceDatasetAdmin(ActorRef database) {
		super(database);	
	}
	
	public static Props props(ActorRef database) {
		return Props.create(SourceDatasetAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		
	}

}
