package nl.idgis.publisher.job.creator;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.messages.JobInfo;

import nl.idgis.publisher.domain.job.JobState;

import nl.idgis.publisher.job.context.messages.JobFinished;
import nl.idgis.publisher.job.creator.messages.CreateHarvestJobs;
import nl.idgis.publisher.job.creator.messages.CreateImportJobs;
import nl.idgis.publisher.job.creator.messages.CreateJobs;
import nl.idgis.publisher.job.creator.messages.CreateServiceJobs;
import nl.idgis.publisher.job.manager.messages.CreateEnsureServiceJob;
import nl.idgis.publisher.job.manager.messages.CreateHarvestJob;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.service.manager.messages.GetServicesWithDataset;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class Creator extends UntypedActor {
		
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef jobManager, database, serviceManager;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public Creator(ActorRef jobManager, ActorRef database, ActorRef serviceManager) {
		this.jobManager = jobManager;
		this.database = database;
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef jobManager, ActorRef database, ActorRef serviceManager) {
		return Props.create(Creator.class, jobManager, database, serviceManager);
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
		} else if(msg instanceof JobFinished) {
			handleJobFinished((JobFinished)msg);
		} else {
			unhandled(msg);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void handleJobFinished(JobFinished msg) {
		log.debug("job finished");
		
		JobInfo jobInfo = msg.getJobInfo();
		JobState jobState = msg.getJobState();
		
		if(jobInfo instanceof VectorImportJobInfo) {
			String datasetId = ((VectorImportJobInfo)jobInfo).getDatasetId();
			
			log.debug("dataset imported: {}", datasetId);
			
			ActorRef jobSystem = getContext().parent();
			f.ask(serviceManager, new GetServicesWithDataset(datasetId), TypedList.class).thenAccept(serviceIds -> {
				log.debug("service ids fetched");
				
				((TypedList<String>)serviceIds).list().stream()
					.forEach(serviceId -> {
						log.debug("creating ensure job for service: {}", serviceId);
						
						jobSystem.tell(new CreateEnsureServiceJob(serviceId), getSelf());
					});
			});
				
					
		} else {
			log.debug("nothing to do");
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
