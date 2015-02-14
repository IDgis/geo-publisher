package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class AdminParent extends AbstractAdminParent {

	private final ActorRef database, harvester, loader, service, jobSystem, serviceManager;
	
	public AdminParent(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem, ActorRef serviceManager) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
		this.jobSystem = jobSystem;
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem, ActorRef serviceManager) {
		return Props.create(AdminParent.class, database, harvester, loader, service, jobSystem, serviceManager);
	}
	
	protected void createActors() {
		getContext().actorOf(Admin.props(database, harvester, loader, service, jobSystem), "admin");
		getContext().actorOf(DataSourceAdmin.props(database, harvester), "data-source");
		getContext().actorOf(CategoryAdmin.props(database), "category");
		getContext().actorOf(DatasetAdmin.props(database), "dataset");
		getContext().actorOf(ServiceAdmin.props(database, serviceManager), "service");
		getContext().actorOf(LayerAdmin.props(database), "layer");
		getContext().actorOf(LayerGroupAdmin.props(database, serviceManager), "layergroup");
		getContext().actorOf(TiledLayerAdmin.props(database), "tiledlayer");
		getContext().actorOf(StyleAdmin.props(database), "style");
		getContext().actorOf(JobCreator.props(database, serviceManager, jobSystem), "job-creator");
	}
}
