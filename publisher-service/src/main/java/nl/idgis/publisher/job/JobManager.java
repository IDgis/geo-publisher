package nl.idgis.publisher.job;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QHarvestJob.harvestJob;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QImportJobColumn.importJobColumn;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QLastSourceDatasetVersion.lastSourceDatasetVersion;
import static nl.idgis.publisher.database.QNotification.notification;
import static nl.idgis.publisher.database.QNotificationResult.notificationResult;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;

import static nl.idgis.publisher.utils.EnumUtils.enumsToStrings;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.DatabaseRef;
import nl.idgis.publisher.database.QJobState;
import nl.idgis.publisher.database.TransactionHandler;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetServiceJobs;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.QHarvestJobInfo;
import nl.idgis.publisher.database.messages.QServiceJobInfo;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedList;
import scala.concurrent.Future;
import scala.runtime.AbstractFunction2;
import scala.runtime.AbstractFunction4;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.pattern.PipeToSupport.PipeableFuture;
import akka.util.Timeout;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.StringPath;

public class JobManager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final DatabaseRef database;
	
	private final Timeout timeout = new Timeout(15, TimeUnit.SECONDS);
	
	public JobManager(ActorRef database) {
		this.database = new DatabaseRef(database, timeout, getContext().dispatcher(), log);	
	}
	
	public static Props props(ActorRef database) {
		return Props.create(JobManager.class, database);
	}
	
	private <T> PipeableFuture<T> pipe(Future<T> future) {
		return Patterns.pipe(future, getContext().dispatcher());
	}
	
	@Override
	public void preStart() throws Exception {
		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("jobs: " + msg);
		
		if(msg instanceof GetImportJobs) {
			handleGetImportJobs();
		} else if(msg instanceof GetHarvestJobs) {
			handleGetHarvestJobs();
		} else if(msg instanceof GetServiceJobs) {
			handleGetServiceJobs();			
		} else if(msg instanceof CreateHarvestJob) {
			pipe(handleCreateHarvestJob((CreateHarvestJob)msg))
				.pipeTo(getSender(), getSelf());
		} else if(msg instanceof CreateImportJob) {
			database.forward(msg, getContext());
		} else if(msg instanceof CreateServiceJob) {
			database.forward(msg, getContext());
		} else if(msg instanceof GetDataSourceStatus) {
			database.forward(msg, getContext());
		} else if(msg instanceof GetDatasetStatus) {
			database.forward(msg, getContext());
		} else {
			unhandled(msg);
		}
	}
	
	private BooleanExpression isFinished(QJobState jobState) {
		return jobState.state.isNull().or(jobState.state.in(enumsToStrings(JobState.getFinished())));
	}
	
	private Future<Ack> handleCreateHarvestJob(final CreateHarvestJob msg) {
		log.debug("creating harvest job: " + msg.getDataSourceId());
		
		return database.transactional(new Function<TransactionHandler, Future<Ack>>() {

			@Override
			public Future<Ack> apply(final TransactionHandler transaction) throws Exception {				
				return transaction.query().from(job)
					.join(harvestJob).on(harvestJob.jobId.eq(job.id))
					.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
					.where(dataSource.identification.eq(msg.getDataSourceId()))
					.where(new SQLSubQuery().from(jobState)
							.where(jobState.jobId.eq(job.id))
							.where(isFinished(jobState))
							.notExists())
					.notExists()
				
				.flatMap(new Mapper<Boolean, Future<Ack>>() {
					
					@Override
					public Future<Ack> apply(Boolean notExists) {
						if(notExists) {
							return transaction.collect(
								transaction.query().from(dataSource)
									.where(dataSource.identification.eq(msg.getDataSourceId()))
									.singleResult(dataSource.id))
							.collect(
								transaction.insert(job)
									.set(job.type, "HARVEST")
									.executeWithKey(job.id))
									
							.flatResult(new AbstractFunction2<Integer, Integer, Future<Long>>() {

								@Override
								public Future<Long> apply(Integer jobId, Integer dataSourceId) {
									log.debug("job created and dataSourceId determined");
									
									return 
										transaction.insert(harvestJob)
											.set(harvestJob.jobId, jobId)				
											.set(harvestJob.dataSourceId, dataSourceId)
											.execute();
								}
								
							})
							
							.returnValue().map(new Mapper<Long, Ack>() {
								
								@Override
								public Ack apply(Long l) {
									log.debug("harvest job created");
									
									return new Ack();
								}
								
							}, getContext().dispatcher());
							
						} else {
							log.debug("already exist a harvest job for this dataSource");
							
							return Futures.successful(new Ack());
						}
					}
					
				}, getContext().dispatcher());
			}			
		});
	}
	
	private void handleGetServiceJobs() {
		log.debug("fetching service jobs");
		
		pipe(
			database.query().from(serviceJob)
				.join(dataset).on(dataset.id.eq(serviceJob.datasetId))
				.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(serviceJob.sourceDatasetVersionId))
				.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
				.where(new SQLSubQuery().from(jobState)
						.where(jobState.jobId.eq(serviceJob.jobId))
						.notExists())
				.list(new QServiceJobInfo(
						serviceJob.jobId, 
						category.identification, 
						dataset.identification)))
						
			.pipeTo(getSender(), getSelf());		
	}

	private void handleGetImportJobs() {
		log.debug("fetching import jobs");
		
		pipe(
			database.transactional(new Function<TransactionHandler, Future<TypedList<ImportJobInfo>>>() {
	
				@Override
				public Future<TypedList<ImportJobInfo>> apply(TransactionHandler transaction) throws Exception {
					AsyncSQLQuery query = transaction.query().from(job)
						.join(importJob).on(importJob.jobId.eq(job.id))			
						.join(dataset).on(dataset.id.eq(importJob.datasetId))
						.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(importJob.sourceDatasetVersionId))
						.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
						.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
						.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
						.orderBy(job.id.asc(), job.createTime.asc())
						.where(new SQLSubQuery().from(jobState)
								.where(jobState.jobId.eq(job.id))
								.notExists());
					
					return transaction.collect(
						query.clone()			
							.list(
								job.id,
								importJob.filterConditions,
								category.identification,
								dataSource.identification,
								sourceDataset.identification,
								dataset.id,
								dataset.name,
								dataset.identification))
					
					.collect(
						query.clone()
							.join(importJobColumn).on(importJobColumn.importJobId.eq(importJob.id))							
							.orderBy(importJobColumn.index.asc())
							.list(job.id, importJobColumn.name, importJobColumn.dataType))
						
					.collect(
						query.clone()
							.join(lastSourceDatasetVersion).on(lastSourceDatasetVersion.datasetId.eq(dataset.id))
							.join(sourceDatasetVersionColumn).on(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(lastSourceDatasetVersion.sourceDatasetVersionId))
							.orderBy(sourceDatasetVersionColumn.index.asc())
							.list(job.id, sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.dataType))
						
					.collect(
						query.clone()
							.join(notification).on(notification.jobId.eq(job.id))
							.leftJoin(notificationResult).on(notificationResult.notificationId.eq(notification.id))
							.list(job.id, notification.type, notificationResult.result))
						
					.result(new AbstractFunction4<TypedList<Tuple>, TypedList<Tuple>, TypedList<Tuple>, TypedList<Tuple>, TypedList<ImportJobInfo>>() {
	
						@Override
						public TypedList<ImportJobInfo> apply(							
							TypedList<Tuple> baseList,						
							TypedList<Tuple> importJobColumnsList, 
							TypedList<Tuple> sourceDatasetColumnsList, 
							TypedList<Tuple> jobNotificationsList) {
							
							ArrayList<ImportJobInfo> jobs = new ArrayList<>();
							
							ListIterator<Tuple> importJobColumns = importJobColumnsList.listIterator();
							ListIterator<Tuple> sourceDatasetColumns = sourceDatasetColumnsList.listIterator();
							ListIterator<Tuple> jobNotifications = jobNotificationsList.listIterator();
							
							for(Tuple t : baseList) {
								int jobId = t.get(job.id);
								
								List<Notification> notifications = new ArrayList<>();
								for(; jobNotifications.hasNext();) {
									Tuple tn = jobNotifications.next();
									
									int notificationJobId = tn.get(job.id);				
									if(notificationJobId != jobId) {
										jobNotifications.previous();
										break;
									}
									
									ImportNotificationType notificationType = ImportNotificationType.valueOf(tn.get(notification.type));
									
									NotificationResult result;
									String resultName = tn.get(notificationResult.result);
									if(resultName == null) {
										result = null;
									} else {
										result = notificationType.getResult(resultName);
									}
									
									notifications.add(new Notification(notificationType, result));
								}
								
								jobs.add(new ImportJobInfo(
										t.get(job.id),
										t.get(category.identification),
										t.get(dataSource.identification), 
										t.get(sourceDataset.identification),
										t.get(dataset.identification),
										t.get(dataset.name),
										t.get(importJob.filterConditions),
										getColumns(importJobColumns, jobId, importJobColumn.name, importJobColumn.dataType),
										getColumns(sourceDatasetColumns, jobId, sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.dataType),
										notifications));
							}						
	
							return new TypedList<>(ImportJobInfo.class, jobs);
						}
						
					})
					
					.returnValue();				
				}
				
			}))
			
			.pipeTo(getSender(), getSelf());
	}
	
	private List<Column> getColumns(ListIterator<Tuple> columnIterator, int jobId, StringPath name, StringPath dataType) {
		List<Column> importJobColumns = new ArrayList<>();
		for(; columnIterator.hasNext();) {
			Tuple tc = columnIterator.next();
			
			int columnJobId = tc.get(job.id);				
			if(columnJobId != jobId) {
				columnIterator.previous();
				break;
			}
			
			importJobColumns.add(new Column(
					tc.get(name), 
					tc.get(dataType)));
		}
		return importJobColumns;
	}
	
	private void handleGetHarvestJobs() {
		log.debug("fetching harvest jobs");
		
		pipe(
			database.query().from(job)
				.join(harvestJob).on(harvestJob.jobId.eq(job.id))
				.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
				.orderBy(job.createTime.asc())
				.where(new SQLSubQuery().from(jobState)
						.where(jobState.jobId.eq(job.id))
						.notExists())
				.list(new QHarvestJobInfo(job.id, dataSource.identification)))
			
			.pipeTo(getSender(), getSelf());
	}
}
