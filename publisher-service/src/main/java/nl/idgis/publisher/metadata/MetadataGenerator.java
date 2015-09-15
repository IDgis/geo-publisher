package nl.idgis.publisher.metadata;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.Kill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.util.Timeout;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncHelper;

import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.metadata.messages.GenerateMetadataEnvironment;
import nl.idgis.publisher.metadata.messages.MetadataInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QService.service;

/**
 * This actor is responsible for initializing the metadata generation. 
 * It is activated by sending a {@link GenerateMetadata} message.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataGenerator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
	private final ActorRef database, metadataSource;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public MetadataGenerator(ActorRef database, ActorRef metadataSource) {
		this.database = database;
		this.metadataSource = metadataSource;		
	}
	
	/**
	 * Creates a {@link Props} for the {@link MetadataGenerator} actor.
	 * 
	 * @param database a reference to the database actor.
	 * @param metadataSource a reference to the metadata source actor. 
	 * @return the props.
	 */
	public static Props props(ActorRef database, ActorRef metadataSource) {
		return Props.create(
			MetadataGenerator.class, 
			Objects.requireNonNull(database, "database must not be null"), 
			Objects.requireNonNull(metadataSource, "metadataSource must not be null"));
	}
	
	@Override
	public void preStart() throws Exception {		
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GenerateMetadata) {
			handleGenerateMetadata((GenerateMetadata)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private static class MetadataUpdateResult {
		
		final long inserted;
		
		MetadataUpdateResult(long inserted) {
			this.inserted = inserted;			
		}

		@Override
		public String toString() {
			return "MetadataUpdateResult [inserted=" + inserted + "]";
		}
	}
	
	private static class GeneratorResult {
		
		final MetadataUpdateResult metadataUpdateResult;
				
		final Collection<Failure> failures;
		
		GeneratorResult(MetadataUpdateResult metadataUpdateResult, Collection<Failure> failures) {			
			this.metadataUpdateResult = metadataUpdateResult;
			this.failures = failures;
		}
		
		Failure getFailure() {
			if(failures.isEmpty()) {
				throw new IllegalStateException("no failure");
			}
			
			if(failures.size() == 1) {
				return failures.iterator().next();
			}
			
			return new Failure(failures);
		}
		
		boolean hasFailure() {
			return !failures.isEmpty();
		}

		@Override
		public String toString() {
			return "GeneratorResult [metadataUpdateResult=" + metadataUpdateResult + ", failures=" + failures + "]";
		}
	}
	
	private Procedure<Object> generatingMetadata() {
		return new Procedure<Object>() {
			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof GenerateMetadataEnvironment) {					
					handleGenerateMetadataEnvironment((GenerateMetadataEnvironment)msg);
				} else if(msg instanceof GeneratorResult) {
					handleGeneratorResult((GeneratorResult)msg);
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private void handleGeneratorResult(GeneratorResult msg) {
		log.debug("generator result received: {}", msg);
		
		if(msg.hasFailure()) {
			Failure failure = msg.getFailure();
			log.error("generator failure: {}", failure);
			getSender().tell(failure, getSelf());
		} else {
			getSender().tell(new Ack(), getSelf());
		}
				
		getContext().become(receive());
	}
	
	private CompletableFuture<Stream<Long>> updateServiceMetadata(AsyncHelper tx) {
		return tx.query().from(service)
			.where(service.wmsMetadataFileIdentification.isNull()
				.or(service.wfsMetadataFileIdentification.isNull()))
			.list(service.id).thenCompose(serviceIds ->
				serviceIds.list().stream()
					.map(serviceId -> 
						tx.update(service)
							.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
							.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
							.where(service.id.eq(serviceId))
							.execute())
					.collect(f.collect()));
	}
	
	private CompletableFuture<Stream<Long>> updateDatasetMetadata(AsyncHelper tx) {
		return tx.query().from(dataset)
			.where(dataset.metadataFileIdentification.isNull()
				.or(dataset.metadataIdentification.isNull()))
			.list(dataset.id).thenCompose(datasetIds ->
				datasetIds.list().stream()
					.map(datasetId ->						
						tx.update(dataset)
							.set(dataset.metadataFileIdentification, UUID.randomUUID().toString())
							.set(dataset.metadataIdentification, UUID.randomUUID().toString())
							.where(dataset.id.eq(datasetId))
							.execute())
					.collect(f.collect()));
	}
	
	private CompletableFuture<MetadataUpdateResult> updateMetadata() {
		return 
			db.transactional(tx ->
				f.concat(
					updateDatasetMetadata(tx),
					updateServiceMetadata(tx))).thenApply(result ->
						result
							.mapToLong(Long::longValue)
							.sum()).thenApply(updateResult ->
								new MetadataUpdateResult(
									updateResult));
	}

	private void handleGenerateMetadata(GenerateMetadata msg) {
		log.info("generating metadata: {}", msg);
		
		ActorRef sender = getSender();
		
		updateMetadata().thenCompose(metadataUpdateResult ->
		
		msg.getEnvironments().stream()
			.map(environment -> 
				f.ask(
					getSelf(), 
					environment, 
					Timeout.apply(Duration.create(30, TimeUnit.MINUTES))))
			.collect(f.collect()).thenApply(responses ->
				new GeneratorResult(
					metadataUpdateResult,
					responses
						.filter(response -> response instanceof Failure)
						.map(response -> (Failure)response)
						.collect(Collectors.toSet())))
				.whenComplete((result, throwable) -> { 
					if(throwable == null) {
						getSelf().tell(result, sender);
					} else {
						log.error("future failed: {}", throwable);
						getSelf().tell(Kill.getInstance(), getSelf());
					}
				}));
		
		getContext().become(generatingMetadata());
	}

	private void handleGenerateMetadataEnvironment(GenerateMetadataEnvironment msg) throws Exception {		
		String environmentId = msg.getEnvironmentId();
		
		log.info("generating metadata for environment: {}", environmentId);
		
		ActorRef processor = getContext().actorOf(
			MetadataInfoProcessor.props(
				getSender(), 
				metadataSource,
				msg.getTarget(),
				msg.getServiceLinkagePrefix(),
				msg.getDatasetMetadataPrefix()),
			
			nameGenerator.getName(MetadataInfoProcessor.class));

		db.transactional(tx -> MetadataInfo.fetch(tx, environmentId))
			.whenComplete((metadataInfo, throwable) ->				 
				processor.tell(
					throwable == null 
						? metadataInfo 
						: new Failure(throwable),
					getSelf()));
	}
}