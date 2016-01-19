package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class AdminParent extends AbstractAdminParent {

	private final ActorRef database, harvester, loader, provisioning, jobManager, serviceManager;
	
	public AdminParent(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef provisioning, ActorRef jobManager, ActorRef serviceManager) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.provisioning = provisioning;
		this.jobManager = jobManager;
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef provisioning, ActorRef jobManager, ActorRef serviceManager) {
		return Props.create(AdminParent.class, database, harvester, loader, provisioning, jobManager, serviceManager);
	}
	
	protected void createActors() {
		createAdminActor(Admin.props(database, harvester, loader, provisioning), "admin");
		createAdminActor(DataSourceAdmin.props(database, harvester), "data-source");
		createAdminActor(CategoryAdmin.props(database), "category");
		createAdminActor(DatasetAdmin.props(database), "dataset");
		createAdminActor(ServiceAdmin.props(database, serviceManager), "service");
		createAdminActor(LayerAdmin.props(database), "layer");
		createAdminActor(LayerGroupAdmin.props(database, serviceManager), "layergroup");
		createAdminActor(TiledLayerAdmin.props(database), "tiledlayer");
		createAdminActor(StyleAdmin.props(database), "style");
		createAdminActor(JobCreator.props(database, serviceManager, jobManager, harvester), "job-creator");
		createAdminActor(ConstantsAdmin.props(database), "constant");
		createAdminActor(SourceDatasetAdmin.props(database), "source-dataset");
	}
}
