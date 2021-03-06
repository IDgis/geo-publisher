package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

public class AdminParent extends AbstractAdminParent {

	private final ActorRef database, harvester, loader, provisioning, jobManager, serviceManager;
	private final String ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations;
	
	public AdminParent(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef provisioning, ActorRef jobManager, ActorRef serviceManager, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers, String ldapApiAdminUrlBaseOrganizations) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.provisioning = provisioning;
		this.jobManager = jobManager;
		this.serviceManager = serviceManager;
		this.ldapApiAdminMail = ldapApiAdminMail;
		this.ldapApiAdminPassword = ldapApiAdminPassword;
		this.ldapApiAdminUrlBaseUsers = ldapApiAdminUrlBaseUsers;
		this.ldapApiAdminUrlBaseOrganizations = ldapApiAdminUrlBaseOrganizations;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef provisioning, ActorRef jobManager, ActorRef serviceManager, String ldapApiAdminMail, String ldapApiAdminPassword, String ldapApiAdminUrlBaseUsers, String ldapApiAdminUrlBaseOrganizations) {
		return Props.create(AdminParent.class, database, harvester, loader, provisioning, jobManager, serviceManager, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations);
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
		createAdminActor(LdapUserAdmin.props(database, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations), "ldap-user");
		createAdminActor(LdapUserGroupAdmin.props(database, ldapApiAdminMail, ldapApiAdminPassword, ldapApiAdminUrlBaseUsers, ldapApiAdminUrlBaseOrganizations), "ldap-usergroup");
	}
}
