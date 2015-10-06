package nl.idgis.publisher.service.provisioning;

import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QService.service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncTransactionRef;

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
	
	private Function<TypedList<String>, CompletableFuture<Stream<Ack>>> createEnsureServiceJobs(AsyncTransactionRef txRef, boolean published) {
		return serviceIds ->
			f.ask(
				jobManager, 
				new CreateVacuumServiceJob(
					Optional.of(txRef),
					published),
				Ack.class).thenCompose(vaccuumAck ->
					serviceIds.asCollection().stream()
						.map(serviceId ->
							f.ask(
								jobManager, 
								new CreateEnsureServiceJob(
									Optional.of(txRef),
									serviceId, 
									published), 
								Ack.class))
					.collect(f.collect())
						.thenApply(ensureAcks ->
							Stream.concat(
								Stream.of(vaccuumAck),
								ensureAcks)));
	}
	
	private void ack(CompletableFuture<Stream<Ack>> acksFuture, ActorRef sender) {
		acksFuture.thenAccept(acks -> {
			log.info("service jobs created: {}", acks.count());
			
			sender.tell(new Ack(), getSelf());
		});
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof AddPublicationService) {
			log.info("publication service added: {}", msg);
						
			String environmentId = ((AddPublicationService) msg).getEnvironmentId();
			
			ack(
				db.transactional(tx ->
					tx.query().from(service)
						.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
						.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(service.id))
						.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
						.where(environment.identification.eq(environmentId))
						.list(genericLayer.identification).thenCompose(createEnsureServiceJobs(
							tx.getTransactionRef(),
							true))),
				getSender());
					
		} else if(msg instanceof AddStagingService) {
			log.info("staging service added: {}", msg);
						
			ack(
				db.transactional(tx ->
					tx.query().from(service)
						.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
						.list(genericLayer.identification).thenCompose(createEnsureServiceJobs(
							tx.getTransactionRef(),
							false))),
				getSender());
		} else {		
			unhandled(msg);
		}
	}

}
