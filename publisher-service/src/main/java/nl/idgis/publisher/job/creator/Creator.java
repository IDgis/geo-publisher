package nl.idgis.publisher.job.creator;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.expr.BooleanExpression;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.QImportJob;
import nl.idgis.publisher.database.QSourceDatasetVersion;
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
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
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
		db = new AsyncDatabaseHelper(database, getClass().getName(), f, log);
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
		
		if(JobState.SUCCEEDED.equals(jobState) && jobInfo instanceof ImportJobInfo) {
			String datasetId = ((ImportJobInfo)jobInfo).getDatasetId();
			
			log.debug("dataset imported: {}", datasetId);
			
			f.ask(serviceManager, new GetServicesWithDataset(datasetId), TypedList.class).thenAccept(serviceIds -> {
				log.debug("service ids fetched");
				
				((TypedList<String>)serviceIds).list().stream()
					.forEach(serviceId -> {
						log.debug("creating ensure job for service: {}", serviceId);
						
						jobManager.tell(new CreateEnsureServiceJob(serviceId), getSelf());
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
		String now = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
		Timestamp ts = Timestamp.valueOf(now + " 00:00:00");
		log.debug("timestamp to check if daily datasets need to be imported: " + ts.toString());
		
		BooleanExpression needsRefresh = datasetStatus.imported.isFalse()
			.or(datasetStatus.sourceDatasetRevisionChanged.isTrue())
			.or(datasetStatus.sourceDatasetColumnsChanged.isTrue())
			.or(datasetStatus.columnsChanged.isTrue())
			.or(datasetStatus.sourceDatasetChanged.isTrue())
			.or(datasetStatus.filterConditionChanged.isTrue());
		
		needsRefresh = needsRefresh.or(sourceDatasetVersion.refreshFrequency.eq("daily")
				.and(job.createTime.before(ts)));
		
		final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
		final QImportJob importJobSub = new QImportJob("import_job_sub");
		
		return db.query().from(datasetStatus)
			.join(dataset).on(dataset.identification.eq(datasetStatus.identification))
			.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(dataset.sourceDatasetId))
			.leftJoin(importJob).on(importJob.datasetId.eq(dataset.id))
			.leftJoin(job).on(job.id.eq(importJob.jobId))
			.where(sourceDatasetVersion.id.eq(new SQLSubQuery()
					.from(sourceDatasetVersionSub)
					.where(sourceDatasetVersionSub.sourceDatasetId.eq(dataset.sourceDatasetId))
					.unique(sourceDatasetVersionSub.id.max()))
				.and(job.id.eq(new SQLSubQuery()
						.from(importJobSub)
						.where(importJobSub.datasetId.eq(dataset.id))
						.unique(importJobSub.jobId.max()))
					.or(job.id.isNull()))
				.and(datasetStatus.sourceDatasetAvailable.isTrue())
				.and(needsRefresh))
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
						log.error("job creation failure: {}", resp);
						
						return f.successful(resp);
					} else {
						return func.apply(t);
					}
				});
			},
			(a, b) -> a.thenCompose(resp -> {
				if(resp instanceof Failure) {
					log.error("job creation failure: {}", resp);
					
					return f.successful(resp);
				} else {
					return b;
				}
			}));
	}
}
