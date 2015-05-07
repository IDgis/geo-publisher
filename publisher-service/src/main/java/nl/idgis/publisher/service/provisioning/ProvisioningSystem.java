package nl.idgis.publisher.service.provisioning;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.service.provisioning.ConnectionInfo;
import nl.idgis.publisher.service.provisioning.ServiceInfo;

public class ProvisioningSystem extends UntypedActor {
	
	private final static SupervisorStrategy supervisorStrategy = new AllForOneStrategy(10, Duration.create("1 minute"), 
		new Function<Throwable, Directive>() {

		@Override
		public Directive apply(Throwable t) throws Exception {			
			return AllForOneStrategy.restart();
		}
		
	});
	
	private final ActorRef database, serviceManager;
	
	private final Config geoserverConfig, databaseConfig, zooKeeperConfig;
	
	private final String rasterFolderConfig;
	
	private ActorRef provisioningManager;
	
	public ProvisioningSystem(ActorRef database, ActorRef serviceManager, Config geoserverConfig, 
		Config databaseConfig, String rasterFolderConfig, Config zooKeeperConfig) {
		
		this.database = database;
		this.serviceManager = serviceManager;
		this.geoserverConfig = geoserverConfig;
		this.databaseConfig = databaseConfig;
		this.rasterFolderConfig = rasterFolderConfig;
		this.zooKeeperConfig = zooKeeperConfig;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, Config geoserverConfig, 
		Config databaseConfig, String rasterFolderConfig, Config zooKeeperConfig) {
		
		return Props.create(ProvisioningSystem.class, database, serviceManager, geoserverConfig, 
			databaseConfig, rasterFolderConfig, zooKeeperConfig);
	}
	
	@Override
	public void preStart() throws Exception {
		provisioningManager = getContext().actorOf(
			ProvisioningManager.props(database, serviceManager, new DefaultProvisioningPropsFactory()), 
			"provisioning-manager");
		
		getContext ().actorOf (ZooKeeperServiceInfoProvider.props (
				new ServiceInfo(
						new ConnectionInfo(
							geoserverConfig.getString("url"),
							geoserverConfig.getString("user"),
							geoserverConfig.getString("password")),
							
						new ConnectionInfo(		
							databaseConfig.getString("url"),
							databaseConfig.getString("user"),
							databaseConfig.getString("password")),
						
						rasterFolderConfig
					),
				provisioningManager,
				zooKeeperConfig.getString ("hosts"),
				zooKeeperConfig.getString ("stagingEnvironmentId"),
				zooKeeperConfig.hasPath ("serviceIdPrefix") ? zooKeeperConfig.getString ("serviceIdPrefix") : null,
				zooKeeperConfig.hasPath ("namespace") ? zooKeeperConfig.getString ("namespace") : null
			), "zookeeper-service-info-provider");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		provisioningManager.forward(msg, getContext());
	}
	
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}
	
}
