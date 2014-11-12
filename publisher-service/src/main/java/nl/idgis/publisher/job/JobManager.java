package nl.idgis.publisher.job;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
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
import static nl.idgis.publisher.database.DatabaseUtils.consumeList;

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
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetServiceJobs;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.QHarvestJobInfo;
import nl.idgis.publisher.database.messages.QServiceJobInfo;
import nl.idgis.publisher.database.messages.ServiceJobInfo;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.service.Column;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.FutureUtils.Collector2;
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
import akka.util.Timeout;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.expr.BooleanExpression;

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
	
	private <T> void returnToSender(Future<T> future) {
		Patterns.pipe(future, getContext().dispatcher())
			.pipeTo(getSender(), getSelf());
	}
	
	@Override
	public void preStart() throws Exception {
		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("jobs: " + msg);
		
		if(msg instanceof GetImportJobs) {
			returnToSender(handleGetImportJobs());
		} else if(msg instanceof GetHarvestJobs) {
			returnToSender(handleGetHarvestJobs());
		} else if(msg instanceof GetServiceJobs) {
			returnToSender(handleGetServiceJobs());			
		} else if(msg instanceof CreateHarvestJob) {
			returnToSender(handleCreateHarvestJob((CreateHarvestJob)msg));			
		} else if(msg instanceof CreateImportJob) {
			returnToSender(handleCreateImportJob((CreateImportJob)msg));			
		} else if(msg instanceof CreateServiceJob) {
			returnToSender(handleCreateServiceJob((CreateServiceJob)msg));
		} else {
			unhandled(msg);
		}
	}
	
	private BooleanExpression isFinished(QJobState jobState) {
		return jobState.state.isNull().or(jobState.state.in(enumsToStrings(JobState.getFinished())));
	}
	
	private Future<Ack> handleCreateServiceJob(CreateServiceJob msg) {
		final String datasetId = msg.getDatasetId();
		
		log.debug("creating service job: " + datasetId);
		
		return database.transactional(new Function<TransactionHandler, Future<Ack>>() {

			@Override
			public Future<Ack> apply(final TransactionHandler transaction) throws Exception {
				return transaction.query().from(job)
					.join(serviceJob).on(serviceJob.jobId.eq(job.id))
					.join(dataset).on(dataset.id.eq(serviceJob.datasetId))
					.where(dataset.identification.eq(datasetId))
					.where(new SQLSubQuery().from(jobState)
							.where(jobState.jobId.eq(job.id))
							.where(isFinished(jobState))
							.notExists())
					.notExists()
				
				.flatMap(new Mapper<Boolean, Future<Ack>>() {
					
					public Future<Ack> apply(Boolean notExists) {
						if(notExists) {
							return createServiceJob(transaction, datasetId)
								.map(new Mapper<Long, Ack>() {
									
									@Override
									public Ack apply(Long l) {
										log.debug("service job created");
										
										return new Ack();
									}
								}, getContext().dispatcher());
						} else {
							log.debug("already exist a service job for this dataset");
							return Futures.successful(new Ack());
						}
					}
				}, getContext().dispatcher());
			}
			
		});
	}
	
	private Future<Long> createServiceJob(final TransactionHandler transaction, final String datasetId) {
		return createJobForDataset(transaction, datasetId)		
			.flatResult(new AbstractFunction2<Integer, Integer, Future<Long>>() {

				@Override
				public Future<Long> apply(Integer jobId, Integer datasetVersionId) {
					return transaction.insert(serviceJob)
						.columns(
							serviceJob.jobId,
							serviceJob.datasetId,
							serviceJob.sourceDatasetVersionId)
						.select(new SQLSubQuery().from(dataset)
							.where(dataset.identification.eq(datasetId))
							.list(jobId, dataset.id, datasetVersionId))						
						.execute();
				}
				
			})
				
			.returnValue();
	}
	
	private Future<Long> createImportJob(final TransactionHandler transaction, final String datasetId) {
		return createJobForDataset(transaction, datasetId)		
			.flatResult(new AbstractFunction2<Integer, Integer, Future<Long>>() {
	
				@Override
				public Future<Long> apply(Integer jobId, Integer datasetVersionId) {
					return 
						transaction.insert(importJob)
							.columns(
								importJob.jobId,
								importJob.datasetId,
								importJob.sourceDatasetVersionId,
								importJob.filterConditions)
							.select(new SQLSubQuery().from(dataset)
									.where(dataset.identification.eq(datasetId))
									.list(
										jobId,
										dataset.id,
										datasetVersionId,
										dataset.filterConditions))
							.executeWithKey(importJob.id)
						
						.flatMap(new Mapper<Integer, Future<Long>>() {
							
							@Override
							public Future<Long> apply(Integer importJobId) {
								return transaction.insert(importJobColumn)
									.columns(
										importJobColumn.importJobId,
										importJobColumn.index,
										importJobColumn.name,
										importJobColumn.dataType)
									.select(new SQLSubQuery().from(datasetColumn)
										.join(dataset).on(dataset.id.eq(datasetColumn.datasetId))
										.where(dataset.identification.eq(datasetId))
										.list(
											importJobId,
											datasetColumn.index,
											datasetColumn.name,
											datasetColumn.dataType))
											.execute();
							}
						}, getContext().dispatcher());										
					}
			})
			
			.returnValue();
	}

	private Collector2<Integer, Integer> createJobForDataset(final TransactionHandler transaction, final String datasetId) {
		return 
			transaction.collect(
				transaction.insert(job)
					.set(job.type, "IMPORT")
					.executeWithKey(job.id))
			.collect(							
				transaction.query().from(sourceDatasetVersion)
					.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
					.join(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
					.where(dataset.identification.eq(datasetId))
					.singleResult(sourceDatasetVersion.id.max()));
	}
	
	private Future<Ack> handleCreateImportJob(CreateImportJob msg) {
		final String datasetId = msg.getDatasetId();
		
		log.debug("creating import job: " + datasetId);
		
		return database.transactional(new Function<TransactionHandler, Future<Ack>>() {

			@Override
			public Future<Ack> apply(final TransactionHandler transaction) throws Exception {
				return transaction.query().from(job)
					.join(importJob).on(importJob.jobId.eq(job.id))
					.join(dataset).on(dataset.id.eq(importJob.datasetId))
					.where(dataset.identification.eq(datasetId))
					.where(new SQLSubQuery().from(jobState)
							.where(jobState.jobId.eq(job.id))
							.where(isFinished(jobState))
							.notExists())
					.notExists()
					
				.flatMap(new Mapper<Boolean, Future<Ack>>() {
					
					@Override
					public Future<Ack> apply(Boolean notExists) {
						if(notExists) {
							return createImportJob(transaction, datasetId)									
								.map(new Mapper<Long, Ack>() {
									
									@Override
									public Ack apply(Long l) {
										log.debug("import job created");
										
										return new Ack();
									}
								}, getContext().dispatcher());
						} else {
							log.debug("already exist an import job for this dataset");
							return Futures.successful(new Ack());
						}
					}					
					
				}, getContext().dispatcher());
			}
			
		});
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
	
	private Future<TypedList<ServiceJobInfo>> handleGetServiceJobs() {
		log.debug("fetching service jobs");
		
		return
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
						dataset.identification));
	}

	private Future<TypedList<ImportJobInfo>> handleGetImportJobs() {
		log.debug("fetching import jobs");
		
		return
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
										consumeList(importJobColumns, jobId, job.id, new Mapper<Tuple, Column>() {
											
											@Override
											public Column apply(Tuple t) {
												return new Column(
													t.get(importJobColumn.name), 
													t.get(importJobColumn.dataType));
											}
										}),
										consumeList(sourceDatasetColumns, jobId, job.id, new Mapper<Tuple, Column>() {
											
											@Override
											public Column apply(Tuple t) {
												return new Column(
													t.get(sourceDatasetVersionColumn.name),
													t.get(sourceDatasetVersionColumn.dataType));
											} 
										}),
										notifications));
							}						
	
							return new TypedList<>(ImportJobInfo.class, jobs);
						}
						
					})
					
					.returnValue();				
				}
				
			});
	}
	
	private Future<TypedList<HarvestJobInfo>> handleGetHarvestJobs() {
		log.debug("fetching harvest jobs");

		return
			database.query().from(job)
				.join(harvestJob).on(harvestJob.jobId.eq(job.id))
				.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
				.orderBy(job.createTime.asc())
				.where(new SQLSubQuery().from(jobState)
						.where(jobState.jobId.eq(job.id))
						.notExists())
				.list(new QHarvestJobInfo(job.id, dataSource.identification));
	}
}
