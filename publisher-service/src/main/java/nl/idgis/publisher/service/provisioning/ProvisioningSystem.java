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
	
	private final Config geoserverConfig, zooKeeperConfig;
	
	private final String rasterFolderConfig, metadataUrlPrefix;
	
	private ActorRef provisioningManager;
	
	public ProvisioningSystem(ActorRef database, ActorRef serviceManager, ActorRef jobManager, 
		Config geoserverConfig, String rasterFolderConfig, Config zooKeeperConfig, String metadataUrlPrefix) {
		
		this.database = database;
		this.serviceManager = serviceManager;
		this.jobManager = jobManager;
		this.geoserverConfig = geoserverConfig;
		this.rasterFolderConfig = rasterFolderConfig;
		this.zooKeeperConfig = zooKeeperConfig;
		this.metadataUrlPrefix = metadataUrlPrefix;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, ActorRef jobManager,
		Config geoserverConfig, String rasterFolderConfig, Config zooKeeperConfig, String metadataUrlPrefix) {
		
		return Props.create(ProvisioningSystem.class, database, serviceManager, jobManager, 
			geoserverConfig, rasterFolderConfig, zooKeeperConfig,
			Objects.requireNonNull (metadataUrlPrefix, "metadataUrlPrefix cannot be null"));
	}
	
	private ActorRef zookeeperServiceInfoListener(ActorRef... refs) {
		if(refs.length == 0) {
			throw new IllegalArgumentException("refs is empty");
		}
		
		if(refs.length == 1) {
			return refs[0];
		}
		
		return getContext().actorOf(
			new BroadcastGroup(
				Stream.of(refs)
					.map(actorRef -> actorRef.path().toString())
					.collect(Collectors.toSet()))
			.props(),
			"zookeeper-service-info-listeners");
	}
	
	@Override
	public void preStart() throws Exception {
		provisioningManager = getContext().actorOf(
			ProvisioningManager.props(database, serviceManager, new DefaultProvisioningPropsFactory(), metadataUrlPrefix), 
			"provisioning-manager");
		
		// TODO: disabled at the moment
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
				zookeeperServiceInfoListener(
					// initServiceJobCreator, TODO: rethink and re-enable job creator
					provisioningManager),
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
