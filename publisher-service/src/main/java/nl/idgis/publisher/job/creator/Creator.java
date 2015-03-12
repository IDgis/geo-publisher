package nl.idgis.publisher.job.creator;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.job.creator.messages.CreateHarvestJobs;
import nl.idgis.publisher.job.creator.messages.CreateImportJobs;
import nl.idgis.publisher.job.creator.messages.CreateJobs;
import nl.idgis.publisher.job.creator.messages.CreateServiceJobs;
import nl.idgis.publisher.job.manager.messages.CreateHarvestJob;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

public class Creator extends UntypedActor {
		
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef jobManager, database;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public Creator(ActorRef jobManager, ActorRef database) {
		this.jobManager = jobManager;
		this.database = database;
	}
	
	public static Props props(ActorRef jobManager, ActorRef database) {
		return Props.create(Creator.class, jobManager, database);
	}
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof CreateJobs) {
			ActorRef sender = getSender(), self = getSelf();
			
			handleCreateJobs((CreateJobs)msg)
				.exceptionally(t -> new Failure(t))
				.thenAccept(resp -> sender.tell(resp, self));
		} else {
			unhandled(msg);
		}
	}
	
	private CompletableFuture<Object> handleCreateJobs(CreateJobs msg) {
		if(msg instanceof CreateHarvestJobs) {
			return handleCreateHarvestJobs((CreateHarvestJobs)msg);
		} else if(msg instanceof CreateImportJobs) {
			return handleCreateImportJobs((CreateImportJobs)msg);
		} else if(msg instanceof CreateServiceJobs) {
			return handleCreateServiceJobs((CreateServiceJobs)msg);
		} else {
			throw new IllegalArgumentException("unknown create jobs message");
		}
	}

	private CompletableFuture<Object> handleCreateHarvestJobs(CreateHarvestJobs msg) {
		return db.query().from(dataSource)		
			.list(dataSource.identification).thenCompose(dataSourceIds ->
				forEach(dataSourceIds.list().stream(), dataSourceId -> f.ask(jobManager, new CreateHarvestJob(dataSourceId))));
	}
	
	private CompletableFuture<Object> handleCreateImportJobs(CreateImportJobs msg) {
		return db.query().from(datasetStatus)
			.where(datasetStatus.imported.isFalse()
				.or(datasetStatus.sourceDatasetRevisionChanged.isTrue())
				.or(datasetStatus.sourceDatasetColumnsChanged.isTrue())
				.or(datasetStatus.columnsChanged.isTrue())
				.or(datasetStatus.sourceDatasetChanged.isTrue())
				.or(datasetStatus.filterConditionChanged.isTrue()))
			.list(datasetStatus.identification).thenCompose(datasetIds ->
				forEach(datasetIds.list().stream(), datasetId -> f.ask(jobManager, new CreateImportJob(datasetId))));
	}
	
	private CompletableFuture<Object> handleCreateServiceJobs(CreateServiceJobs msg) {
		// TODO: implement support for service jobs
		
		return f.failed(new UnsupportedOperationException());
	}
	
	private <T> CompletableFuture<Object> forEach(Stream<T> stream, Function<T, CompletableFuture<Object>> func) {
		return stream.reduce(
			f.successful((Object)new Ack()),
			(future, t) -> {
				return future.thenCompose(resp -> {
					if(resp instanceof Failure) {
						return f.successful(resp);
					} else {
						return func.apply(t);
					}
				});
			},
			(a, b) -> a.thenCompose(resp -> {
				if(resp instanceof Failure) {
					return f.successful(resp);
				} else {
					return b;
				}
			}));
	}
}
