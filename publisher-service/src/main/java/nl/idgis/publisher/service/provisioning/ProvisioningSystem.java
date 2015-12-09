package nl.idgis.publisher.service.provisioning;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.japi.Function;
import akka.routing.BroadcastGroup;

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
	
	private final ActorRef database, serviceManager, jobManager;
	
	private final Config geoserverConfig, zooKeeperConfig, metadataEnvironmentConfig;
	
	private final String rasterFolderConfig;
	
	private ActorRef provisioningManager;
	
	public ProvisioningSystem(ActorRef database, ActorRef serviceManager, ActorRef jobManager, 
		Config geoserverConfig, String rasterFolderConfig, Config zooKeeperConfig, final Config metadataEnvironmentConfig) {
		
		this.database = database;
		this.serviceManager = serviceManager;
		this.jobManager = jobManager;
		this.geoserverConfig = geoserverConfig;
		this.rasterFolderConfig = rasterFolderConfig;
		this.zooKeeperConfig = zooKeeperConfig;
		this.metadataEnvironmentConfig = metadataEnvironmentConfig;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, ActorRef jobManager,
		Config geoserverConfig, String rasterFolderConfig, Config zooKeeperConfig, final Config metadataEnvironmentConfig) {
		
		return Props.create(ProvisioningSystem.class, database, serviceManager, jobManager, 
			geoserverConfig, rasterFolderConfig, zooKeeperConfig,
			Objects.requireNonNull (metadataEnvironmentConfig, "metadataEnvironmentConfig cannot be null"));
	}
	
	@Override
	public void preStart() throws Exception {
		provisioningManager = getContext().actorOf(
			ProvisioningManager.props(database, serviceManager, new DefaultProvisioningPropsFactory(), metadataEnvironmentConfig), 
			"provisioning-manager");
		
		ActorRef initServiceJobCreator = getContext ().actorOf(
			InitServiceJobCreator.props(database, jobManager), 
			"init-service-job-creator");
		
		getContext ().actorOf (ZooKeeperServiceInfoProvider.props (
				new ServiceInfo(
						new ConnectionInfo(
							geoserverConfig.getString("url"),
							geoserverConfig.getString("user"),
							geoserverConfig.getString("password")),
						rasterFolderConfig
					),
				getContext().actorOf(
					new BroadcastGroup(
						Stream
							.of(
								provisioningManager,
								initServiceJobCreator)
							.map(actorRef -> actorRef.path().toString())
							.collect(Collectors.toSet()))
					.props(),
					"zookeeper-service-info-listeners"),
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
