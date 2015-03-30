package nl.idgis.publisher.service.provisioning;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.EnsureServiceJobInfo;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.job.manager.messages.StoreLog;
import nl.idgis.publisher.job.manager.messages.VacuumServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.geoserver.GeoServerService;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetStyles;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.service.provisioning.messages.AddPublicationService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.service.provisioning.messages.RemovePublicationService;
import nl.idgis.publisher.service.provisioning.messages.RemoveStagingService;
import nl.idgis.publisher.service.provisioning.messages.UpdateServiceInfo;
import nl.idgis.publisher.utils.AskResponse;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import static java.util.Collections.singletonList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

public class ProvisioningManager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
	private final ProvisioningPropsFactory provisioningPropsFactory;
	
	private final ActorRef database, serviceManager;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	private Set<ServiceInfo> staging;
	
	private Map<String, Set<ServiceInfo>> publication;
	
	private Map<ServiceInfo, String> publicationReverse;
	
	private Map<ServiceInfo, ActorRef> services;
	
	public ProvisioningManager(ActorRef database, ActorRef serviceManager, ProvisioningPropsFactory provisioningPropsFactory) {
		this.database = database;
		this.serviceManager = serviceManager;
		this.provisioningPropsFactory = provisioningPropsFactory;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager) {
		return props(database, serviceManager, new DefaultProvisioningPropsFactory());
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
		
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof UpdateServiceInfo) {
			handleUpdateServiceInfo((UpdateServiceInfo)msg);
		} else if(msg instanceof ServiceJobInfo) {			
			handleServiceJobInfo((ServiceJobInfo)msg);
		} else if(msg instanceof GetActiveJobs) {
			getSender().tell(new ActiveJobs(emptyList()), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private void elseProvisioning(Object msg, ServiceJobInfo serviceJob, ActorRef initiator, Optional<ActorRef> watching, Set<ActorRef> targets, Set<JobState> state) {
		if(msg instanceof ServiceJobInfo) {
			// this shouldn't happen too often, TODO: rethink job mechanism
			log.debug("receiving service job while already provisioning");
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof GetActiveJobs) {
			getSender().tell(new ActiveJobs(singletonList(new ActiveJob(serviceJob))), getSelf());
		} else if(msg instanceof UpdateJobState) {
			log.debug("update job state received: {}", msg);
			
			ActorRef target = getSender(); 
			
			if(targets.remove(target)) {
				state.add(((UpdateJobState)msg).getState());
			} else {
				log.error("update job state request received from unknown target: {}", target);
			}
			
			if(targets.isEmpty()) {
				log.debug("all targets reported a state");
				
				if(state.contains(JobState.FAILED)) {
					initiator.tell(new UpdateJobState(JobState.FAILED), getSelf());
				} else if(state.contains(JobState.ABORTED)) {
					initiator.tell(new UpdateJobState(JobState.ABORTED), getSelf());
				} else {
					initiator.tell(new UpdateJobState(JobState.SUCCEEDED), getSelf());
				}
				
				if(watching.isPresent()) {
					getContext().unwatch(watching.get());
				}
				
				getContext().become(receive());
			}
		} else if(msg instanceof StoreLog) {
			initiator.tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> provisioning(ServiceJobInfo serviceJob, ActorRef initiator, ActorRef watching, Set<ActorRef> targets) {
		return new Procedure<Object>() {
			
			Set<JobState> state = new HashSet<>();

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof UpdateServiceInfo) {
					handleUpdateServiceInfo((UpdateServiceInfo)msg);
				} else if(msg instanceof Terminated) {
					log.error("actor terminated unexpectedly");
					
					initiator.tell(new UpdateJobState(JobState.FAILED), getSelf());						
					getContext().become(receive());
				} else {
					elseProvisioning(msg, serviceJob, initiator, Optional.of(watching), targets, state);
				}
			}			
		};
	}
	
	private Procedure<Object> vacuuming(ServiceJobInfo serviceJob, ActorRef initiator, Set<ActorRef> targets) {
		return new Procedure<Object>() {
			
			Set<JobState> state = new HashSet<>();

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof UpdateServiceInfo) {
					handleUpdateServiceInfo((UpdateServiceInfo)msg);
				} else if(msg instanceof ServiceIndex) {
					targets.stream().forEach(target -> target.tell(msg, getSelf()));
				} else {
					elseProvisioning(msg, serviceJob, initiator, Optional.empty(), targets, state);
				}
			}			
		};
	}
	
	private void handleServiceJobInfo(ServiceJobInfo msg) {
		log.debug("service job received");
		
		ActorRef initiator = getSender();
		initiator.tell(new UpdateJobState(JobState.STARTED), getSelf());
		initiator.tell(new Ack(), getSelf());
		
		// TODO: figure out the targets based on the job information
		Set<ActorRef> targets = staging.stream()
			.map(services::get)
			.collect(toSet());
		
		if(msg instanceof EnsureServiceJobInfo) {		
			ActorRef jobHandler = getContext().actorOf(
					provisioningPropsFactory.ensureJobProps(targets),
					nameGenerator.getName(msg.getClass()));
			
			getContext().watch(jobHandler);
			
			String serviceId = ((EnsureServiceJobInfo)msg).getServiceId();
			
			db.<List<AskResponse<Object>>>transactional(tx ->			
			f.askWithSender(serviceManager, new GetService(tx.getTransactionRef(), serviceId)).thenCompose(service ->
			f.askWithSender(serviceManager, new GetStyles(tx.getTransactionRef(), serviceId)).thenApply(styles -> 
				Arrays.asList(service, styles)))).thenAccept(results ->
					results.stream().forEach(result ->
						jobHandler.tell(result.getMessage(), result.getSender())));
			
			getContext().become(provisioning(msg, initiator, jobHandler, targets));
		} else if(msg instanceof VacuumServiceJobInfo) {
			serviceManager.tell(new GetServiceIndex(), getSelf());			
			getContext().become(vacuuming(msg, initiator, targets));
		} else {
			unhandled(msg);
		}
	}

	private void handleUpdateServiceInfo(UpdateServiceInfo msg) {
		log.debug("update service info received");
		
		if(msg instanceof AddStagingService) {
			ServiceInfo serviceInfo = ((AddStagingService)msg).getServiceInfo();
			
			log.info("adding staging service: {}", serviceInfo);
			
			if(staging.contains(serviceInfo)) {
				log.debug("service is already registed");
			} else {
				staging.add(serviceInfo);
				
				createServiceActor(serviceInfo, "staging_data");
			}
			
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof RemoveStagingService) {
			ServiceInfo serviceInfo = ((RemoveStagingService)msg).getServiceInfo();
			
			log.info("removing staging service: {}", serviceInfo);
			
			if(staging.contains(serviceInfo)) {
				staging.remove(serviceInfo);
				
				stopServiceActor(serviceInfo);
			} else {
				log.error("trying to remove an unregistered service");
			}
			
			getSender().tell(new Ack(), getSelf());
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
			
			getSender().tell(new Ack(), getSelf());
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
			
			getSender().tell(new Ack(), getSelf());
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
