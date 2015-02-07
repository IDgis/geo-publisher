package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class AdminParent extends UntypedActor {
	
	private final ActorRef database, harvester, loader, service, jobSystem;
	
	public AdminParent(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
		this.jobSystem = jobSystem;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem) {
		return Props.create(AdminParent.class, database, harvester, loader, service, jobSystem);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().actorOf(Admin.props(database, harvester, loader, service, jobSystem), "admin");
		getContext().actorOf(DataSourceAdmin.props(database, harvester));
		getContext().actorOf(CategoryAdmin.props(database));
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		unhandled(msg);
	}
}
