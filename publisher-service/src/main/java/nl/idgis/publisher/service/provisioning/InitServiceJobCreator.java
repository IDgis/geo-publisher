package nl.idgis.publisher.service.provisioning;

import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QService.service;

import java.util.function.Consumer;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.job.manager.messages.CreateEnsureServiceJob;
import nl.idgis.publisher.job.manager.messages.CreateVacuumServiceJob;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.provisioning.messages.AddPublicationService;
import nl.idgis.publisher.service.provisioning.messages.AddStagingService;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

/**
 * This actor receives {@link AddPublicationService} and 
 * {@link AddStagingService} messages and creates jobs
 * to properly provision these new services.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class InitServiceJobCreator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, jobManager;
	
	private FutureUtils f; 
	
	private AsyncDatabaseHelper db;
	
	public InitServiceJobCreator(ActorRef database, ActorRef jobManager) {
		this.database = database;
		this.jobManager = jobManager;
	}
	
	public static Props props(ActorRef database, ActorRef jobManager) {
		return Props.create(InitServiceJobCreator.class, database, jobManager);
	}
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}
	
	private Consumer<TypedList<String>> createEnsureServiceJobs(ActorRef sender, boolean published) {
		return serviceIds -> {
			serviceIds.asCollection().stream()
				.map(serviceId ->
					f.ask(
						jobManager, 
						new CreateEnsureServiceJob(
							serviceId, 
							published), 
						Ack.class))
				.collect(f.collect()).thenAccept(ack -> {
					log.info("ensure service jobs created: {}", ack.count());
					sender.tell(new Ack(), getSelf());
				});
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof AddPublicationService) {
			log.info("publication service added: {}", msg);
			
			jobManager.tell(new CreateVacuumServiceJob(true), getSelf());
			
			String environmentId = ((AddPublicationService) msg).getEnvironmentId();
			
			db.query().from(service)
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(service.id))
				.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
				.where(environment.identification.eq(environmentId))
				.list(genericLayer.identification).thenAccept(createEnsureServiceJobs(getSender(), true));
					
		} else if(msg instanceof AddStagingService) {
			log.info("staging service added: {}", msg);
			
			jobManager.tell(new CreateVacuumServiceJob(), getSelf());
			
			db.query().from(service)
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.list(genericLayer.identification).thenAccept(createEnsureServiceJobs(getSender(), false));
		} else {		
			unhandled(msg);
		}
	}

}
