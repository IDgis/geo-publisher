package nl.idgis.publisher.service.provisioning;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.geoserver.GeoServerService;
import nl.idgis.publisher.service.provisioning.messages.AddPublicationService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.service.provisioning.messages.RemovePublicationService;
import nl.idgis.publisher.service.provisioning.messages.RemoveStagingService;
import nl.idgis.publisher.service.provisioning.messages.UpdateServiceInfo;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import static java.util.stream.Collectors.toSet;

public class ProvisioningManager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef database, serviceManager;
	
	private final ProvisioningPropsFactory provisioningPropsFactory;
	
	private Set<ServiceInfo> staging;
	
	private Map<String, Set<ServiceInfo>> publication;
	
	private Map<ServiceInfo, String> publicationReverse;
	
	private Map<ServiceInfo, ActorRef> services;
	
	public ProvisioningManager(ActorRef database, ActorRef serviceManager, ProvisioningPropsFactory provisioningPropsFactory) {
		this.database = database;
		this.serviceManager = serviceManager;
		this.provisioningPropsFactory = provisioningPropsFactory;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, ProvisioningPropsFactory provisioningPropsFactory) {
		return Props.create(ProvisioningManager.class, database, serviceManager, provisioningPropsFactory);
	}
	
	@Override
	public final void preStart() throws Exception {
		staging = new HashSet<>();
		publication = new HashMap<>();
		publicationReverse = new HashMap<>();
		services = new HashMap<>();
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof UpdateServiceInfo) {
			handleUpdateServiceInfo((UpdateServiceInfo)msg);
		} else if(msg instanceof ServiceJobInfo) {			
			handleServiceJobInfo((ServiceJobInfo)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> provisioning() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof UpdateServiceInfo) {
					handleUpdateServiceInfo((UpdateServiceInfo)msg);
				} else if(msg instanceof ServiceJobInfo) {
					// this shouldn't happen too often, TODO: rethink job mechanism
					log.debug("receiving service job while already provisioning");
					getSender().tell(new Ack(), getSelf());
				} else if(msg instanceof Terminated) {
					log.debug("provisioning finished");
					
					getContext().become(receive());
				} else {
					unhandled(msg);
				}
			}
		};
	}
	
	private void handleServiceJobInfo(ServiceJobInfo msg) {
		// TODO: figure out the targets based on job information
		Set<ActorRef> targets = staging.stream()
			.map(services::get)
			.collect(toSet());
		
		getContext().watch(
			getContext().actorOf(
				provisioningPropsFactory.jobProps(database, serviceManager, msg, getSender(), targets),
				nameGenerator.getName(msg.getClass())));
		
		getContext().become(provisioning());
	}

	private void handleUpdateServiceInfo(UpdateServiceInfo msg) {
		if(msg instanceof AddStagingService) {
			ServiceInfo serviceInfo = ((AddStagingService)msg).getServiceInfo();
			
			log.info("adding staging service: {}", serviceInfo);
			
			if(staging.contains(serviceInfo)) {
				log.debug("service is already registed");
			} else {
				staging.add(serviceInfo);
				
				createServiceActor(serviceInfo, "staging_data");
			}
		} else if(msg instanceof RemoveStagingService) {
			ServiceInfo serviceInfo = ((RemoveStagingService)msg).getServiceInfo();
			
			log.info("removing staging service: {}", serviceInfo);
			
			if(staging.contains(serviceInfo)) {
				staging.remove(serviceInfo);
				
				stopServiceActor(serviceInfo);
			} else {
				log.error("trying to remove an unregistered service");
			}
		} else if(msg instanceof AddPublicationService) {
			AddPublicationService addPublicationService = (AddPublicationService)msg;
			
			String environmentId = addPublicationService.getEnvironmentId();
			ServiceInfo serviceInfo = addPublicationService.getServiceInfo();
			
			log.info("adding publication service: {} for environment: {}", serviceInfo, environmentId);
			
			if(publicationReverse.containsKey(serviceInfo)) {
				log.debug("service is already registed");
			} else {			
				final Set<ServiceInfo> environmentSet;
				if(publication.containsKey(environmentId)) {
					environmentSet = publication.get(environmentId);
				} else {
					environmentSet = new HashSet<>();
					publication.put(environmentId, environmentSet);
				}
				
				environmentSet.add(serviceInfo);
				publicationReverse.put(serviceInfo, environmentId);
				
				createServiceActor(serviceInfo, "data");
			}
		} else if(msg instanceof RemovePublicationService) {
			RemovePublicationService removedPublicationService = (RemovePublicationService)msg;
			
			ServiceInfo serviceInfo = removedPublicationService.getServiceInfo();
			if(publicationReverse.containsKey(serviceInfo)) {
				String environmentId = publicationReverse.remove(serviceInfo);
				
				log.info("removing publication service: {} for environment: {}", serviceInfo, environmentId);
				
				if(publication.containsKey(environmentId)) {
					Set<ServiceInfo> environmentSet = publication.get(environmentId);
					if(environmentSet.contains(serviceInfo)) {
						environmentSet.remove(serviceInfo);
						
						if(environmentSet.isEmpty()) {
							publication.remove(environmentId);
						}
						
						stopServiceActor(serviceInfo);
					}	
				}
			} else {
				log.error("trying to remove an unregistered service");
			}
		} else {
			unhandled(msg);
		}
	}

	private void stopServiceActor(ServiceInfo serviceInfo) {
		log.debug("stopping actor for service: {}", serviceInfo);
		
		getContext().stop(services.remove(serviceInfo));
	}
	
	private void createServiceActor(ServiceInfo serviceInfo, String schema) {
		log.debug("creating actor for service: {} {}", serviceInfo, schema);
		
		services.put(serviceInfo, 
			getContext().actorOf(
				provisioningPropsFactory.serviceProps(serviceInfo, schema),
				nameGenerator.getName(GeoServerService.class)));
	}
}
