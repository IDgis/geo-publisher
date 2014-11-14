package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetActiveNotification.datasetActiveNotification;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;
import static nl.idgis.publisher.database.QHarvestJob.harvestJob;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobLog.jobLog;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;
import static nl.idgis.publisher.database.QLastServiceJob.lastServiceJob;
import static nl.idgis.publisher.database.QNotification.notification;
import static nl.idgis.publisher.database.QNotificationResult.notificationResult;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumnDiff.sourceDatasetColumnDiff;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QVersion.version;
import static nl.idgis.publisher.utils.EnumUtils.enumsToStrings;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import nl.idgis.publisher.database.messages.AddNotification;
import nl.idgis.publisher.database.messages.AddNotificationResult;
import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.BaseDatasetInfo;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.DeleteDataset;
import nl.idgis.publisher.database.messages.GetCategoryListInfo;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetColumnDiff;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetDatasetListInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetJobLog;
import nl.idgis.publisher.database.messages.GetNotifications;
import nl.idgis.publisher.database.messages.GetSourceDatasetListInfo;
import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.ListQuery;
import nl.idgis.publisher.database.messages.PerformInsert;
import nl.idgis.publisher.database.messages.PerformQuery;
import nl.idgis.publisher.database.messages.QCategoryInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.QDataSourceStatus;
import nl.idgis.publisher.database.messages.QDatasetStatusInfo;
import nl.idgis.publisher.database.messages.QSourceDatasetInfo;
import nl.idgis.publisher.database.messages.QVersion;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.RemoveNotification;
import nl.idgis.publisher.database.messages.ServiceJobInfo;
import nl.idgis.publisher.database.messages.SourceDatasetInfo;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.StoreNotificationResult;
import nl.idgis.publisher.database.messages.StoredJobLog;
import nl.idgis.publisher.database.messages.StoredNotification;
import nl.idgis.publisher.database.messages.TerminateJobs;
import nl.idgis.publisher.database.messages.UpdateDataset;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.database.messages.Updated;
import nl.idgis.publisher.database.projections.QColumn;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;
import nl.idgis.publisher.domain.MessageTypeUtils;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.ColumnDiff;
import nl.idgis.publisher.domain.service.ColumnDiffOperation;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.QueryMetadata;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Order;
import com.mysema.query.types.Path;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.ComparableExpressionBase;
import com.typesafe.config.Config;

public class PublisherTransaction extends QueryDSLTransaction {
	
	private final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public PublisherTransaction(Config config, Connection connection) {
		super(config, connection);
	}
	
	private void insertSourceDatasetColumns(int versionId, List<Column> columns) {
		int i = 0;
		for(Column column : columns) {			
			insert(sourceDatasetVersionColumn)
				.set(sourceDatasetVersionColumn.sourceDatasetVersionId, versionId)
				.set(sourceDatasetVersionColumn.index, i++)
				.set(sourceDatasetVersionColumn.name, column.getName())
				.set(sourceDatasetVersionColumn.dataType, column.getDataType().toString())
				.execute();
		}
	}
	
	private void insertDatasetColumns(int datasetId, List<Column> columns) {
		int i = 0;
		for(Column column : columns) {			
			insert(datasetColumn)
				.set(datasetColumn.datasetId, datasetId)
				.set(datasetColumn.index, i++)
				.set(datasetColumn.name, column.getName())
				.set(datasetColumn.dataType, column.getDataType().toString())
				.execute();
		}
	}
	
	private int getCategoryId(String identification) {
		Integer id = query().from(category)
			.where(category.identification.eq(identification))
			.singleResult(category.id);
		
		if(id == null) {
			insert(category)
				.set(category.identification, identification)
				.set(category.name, identification)
				.execute();
			
			return getCategoryId(identification);
		} else {
			return id;
		}
	}
	
	private <T extends Comparable<? super T>> SQLQuery applyListParams(SQLQuery query, ListQuery listQuery, ComparableExpressionBase<T> orderBy) {
		Order order = listQuery.getOrder();
		Long limit = listQuery.getLimit();
		Long offset = listQuery.getOffset();
		
		if(order != null) {
			if(order == Order.ASC) {
				query = query.orderBy(orderBy.asc());
			} else {
				query = query.orderBy(orderBy.desc());
			}
		}
		
		if(limit != null) {
			query = query.limit(limit);
		}
		
		if(offset != null) {
			query = query.offset(offset);
		}
		
		return query;
	}
	
	@Override
	protected void executeQuery(Query query) throws Exception {
		if(query instanceof GetVersion) {
			executeGetVersion();
		} else if(query instanceof RegisterSourceDataset) {
			executeRegisterSourceDataset((RegisterSourceDataset)query);
		} else if(query instanceof GetCategoryListInfo) {
			executeGetCategoryListInfo();
		} else if(query instanceof GetDatasetListInfo) {
			executeGetDatasetListInfo((GetDatasetListInfo)query);			
		} else if(query instanceof GetDataSourceInfo) {
			executeGetDataSourceInfo();
		} else if(query instanceof GetSourceDatasetListInfo) {			
			executeGetSourceDatasetListInfo((GetSourceDatasetListInfo)query);			
		} else if(query instanceof StoreLog) {
			executeStoreLog((StoreLog)query);		
		} else if(query instanceof CreateDataset) {
			executeCreateDataset((CreateDataset)query);
		} else if(query instanceof GetDatasetInfo) {			
			executeGetDatasetInfo((GetDatasetInfo)query);
		} else if(query instanceof UpdateDataset) {						
			executeUpdatedataset((UpdateDataset)query);
		} else if(query instanceof DeleteDataset) {
			executeDeleteDataset((DeleteDataset)query);
		} else if(query instanceof UpdateJobState) {
			executeUpdateJobState((UpdateJobState)query);
		} else if(query instanceof GetDataSourceStatus) {
			executeGetDataSourceStatus();
		} else if(query instanceof GetJobLog) {
			executeGetJobLog((GetJobLog)query);
		} else if(query instanceof GetDatasetStatus) {
			executeGetDatasetStatus((GetDatasetStatus)query);
		} else if(query instanceof TerminateJobs) {
			executeTerminateJobs();
		} else if(query instanceof AddNotification) {
			executeAddNotification((AddNotification)query);
		} else if(query instanceof AddNotificationResult) {
			executeAddNotificationResult((AddNotificationResult)query);
		} else if(query instanceof RemoveNotification) {
			executeRemoveNotification((RemoveNotification)query);
		} else if (query instanceof GetNotifications) {
			executeGetNotifications ((GetNotifications) query);
		} else if (query instanceof StoreNotificationResult) {
			executeStoreNotificationResult ((StoreNotificationResult) query);
		} else if (query instanceof GetDatasetColumnDiff) {
			executeGetDatasetColumnDiff ((GetDatasetColumnDiff) query);
		} else if (query instanceof PerformQuery) {
			executePerformQuery((PerformQuery)query);
		} else if (query instanceof PerformInsert) {
			executePerformInsert((PerformInsert)query);
		} else {
			unhandled(query);
		}
	}

	private void executePerformQuery(PerformQuery query) {
		QueryMetadata metadata = query.getMetadata();
		
		List<Expression<?>> projection = metadata.getProjection();
		if(projection.size() == 1) {	
			answerQuery(metadata, projection.get(0));
		} else {		
			answer(
				Tuple.class,		
				query(metadata).list(projection.toArray(new Expression<?>[projection.size()])));
		}
	}
	
	private void executePerformInsert(PerformInsert query) {
		SQLInsertClause insert = insert(query.getEntity());
		
		
		Path<?>[] columns = query.getColumns();
		
		SubQueryExpression<?> subQuery = query.getSubQuery();
		if(subQuery != null) {
			insert
				.columns(columns)
				.select(subQuery);
		} else {
			insert
				.columns(columns)
				.values((Object[])query.getValues());
		}
		
		Path<?> key = query.getKey();
		
		if(key != null) {
			answer(insert.executeWithKey(key));
		} else {
			answer(insert.execute());
		}
	}

	private void executeRemoveNotification(RemoveNotification query) {	
		JobInfo job = query.getJob();
		NotificationType<?> type = query.getNotificationType();
		
		delete(notificationResult)
			.where(new SQLSubQuery().from(notification)
				.where(notification.id.eq(notificationResult.notificationId)
					.and(notification.type.eq(type.name())
					.and(notification.jobId.eq(job.getId()))))
				.exists())
			.execute();
		
		delete(notification)
			.where(notification.type.eq(type.name())
				.and(notification.jobId.eq(job.getId())))
			.execute();
		
		ack();
	}

	private void executeAddNotificationResult(AddNotificationResult query) {
		JobInfo job = query.getJob();
		NotificationType<?> type = query.getNotificationType();
		NotificationResult result = query.getNotificationResult();
		
		insert(notificationResult)
			.columns(
				notificationResult.notificationId,
				notificationResult.result)
			.select(new SQLSubQuery().from(notification)
				.where(notification.jobId.eq(job.getId())
					.and(notification.type.eq(type.name())))				
				.list(
					notification.id,
					result.name()))
			.execute();
		
		ack();
	}

	private void executeAddNotification(AddNotification query) {
		JobInfo job = query.getJob();
		NotificationType<?> notificationType = query.getNotificationType();
		
		insert(notification)
			.set(notification.jobId, job.getId())
			.set(notification.type, notificationType.name())
			.execute();
		
		ack();
	}

	private void executeTerminateJobs() {
		final QJobState jobStateSub = new QJobState("job_state_sub");
		
		long result = insert(jobState)
			.columns(
				jobState.jobId,
				jobState.state)
			.select(new SQLSubQuery().from(job)
				.where(new SQLSubQuery().from(jobStateSub)
						.where(jobStateSub.jobId.eq(job.id)
								.and(jobStateSub.state.in(
										enumsToStrings(JobState.getFinished()))))
						.notExists())
				.list(
					job.id, 
					JobState.FAILED.name()))
			.execute();
		
		log.debug("jobs terminated: " + result);
		
		ack();
	}

	private void executeGetDatasetStatus(GetDatasetStatus query) {		
		SQLQuery baseQuery = query().from(datasetStatus)
			.join(dataset).on(dataset.id.eq(datasetStatus.id));
		
		Expression<DatasetStatusInfo> expression = 
				new QDatasetStatusInfo(					
						dataset.identification, 
						datasetStatus.columnsChanged, 
						datasetStatus.filterConditionChanged, 
						datasetStatus.sourceDatasetChanged, 
						datasetStatus.imported, 
						datasetStatus.serviceCreated, 
						datasetStatus.sourceDatasetColumnsChanged, 
						datasetStatus.sourceDatasetRevisionChanged);
		
		String datasourceId = query.getDatasetId();
		if(datasourceId == null) {		
			answer(
				DatasetStatusInfo.class,
				
				baseQuery.list(expression));
		} else {
			answer(
				baseQuery.where(dataset.identification.eq(datasourceId))
				.singleResult(expression));
		}
	}	

	private Integer getDatasetId(String datasetIdentification) {
		return query().from(dataset)
			.where(dataset.identification.eq(datasetIdentification))
			.singleResult(dataset.id);
	}	

	private void executeGetJobLog(GetJobLog query) throws Exception {
		final SQLQuery baseQuery = query().from(jobLog)
				.join(jobState).on(jobState.id.eq(jobLog.jobStateId))
				.join(job).on(job.id.eq(jobState.jobId));
		
		if (query.getLogLevels () != null && !query.getLogLevels ().isEmpty ()) {
			final List<String> logLevelStrings = new ArrayList<> (query.getLogLevels ().size ());
			for (final LogLevel ll: query.getLogLevels ()) {
				logLevelStrings.add (ll.name ());
			}
			
			baseQuery.where (jobLog.level.in (logLevelStrings));
		}
		
		if (query.getSince () != null) {
			baseQuery.where (jobLog.createTime.gt (query.getSince ()));
		}
		
		List<StoredJobLog> jobLogs = new ArrayList<>();
		for(Tuple t : 
			applyListParams(baseQuery.clone (), query, jobLog.createTime)
				.list(
					job.id, 
					job.type,
					jobLog.level, 
					jobLog.type, 
					jobLog.content,
					jobLog.createTime)) {
			
			JobType jobType = JobType.valueOf(t.get(job.type));
			
			JobInfo jobInfo = new JobInfo(
					t.get(job.id), jobType);
			
			LogLevel logLevel = LogLevel.valueOf(t.get(jobLog.level));
			
			Class<? extends MessageType<?>> logTypeClass = jobType.getLogMessageEnum ();
			MessageType<?> logType = MessageTypeUtils.valueOf(logTypeClass, t.get(jobLog.type));
			
			String content = t.get(jobLog.content);
			
			MessageProperties contentObject;
			if(content == null) {
				contentObject = null;
			} else {
				contentObject = fromJson(logType.getContentClass(), content); 
			}			
			
			final Timestamp when = t.get (jobLog.createTime);
			
			jobLogs.add(new StoredJobLog(jobInfo, logLevel, logType, when, contentObject));
		}
		
		answer(new InfoList<StoredJobLog> (jobLogs, baseQuery.count ()));
	}
	
	private void executeGetNotifications (final GetNotifications query) throws Exception {
		
		final SQLQuery baseQuery = query ().from (datasetActiveNotification);
		
		if (!query.isIncludeRejected ()) {
			baseQuery.where (datasetActiveNotification.notificationResult.ne (ConfirmNotificationResult.NOT_OK.name ()));
		}
		
		if (query.getSince () != null) {
			baseQuery.where (datasetActiveNotification.jobCreateTime.gt (query.getSince ()));
		}
		
		final List<StoredNotification> notifications = new ArrayList<> ();
		
		for (final Tuple t: applyListParams (baseQuery.clone (), query, datasetActiveNotification.jobCreateTime)
				.list (
					datasetActiveNotification.notificationId,
					datasetActiveNotification.notificationType,
					datasetActiveNotification.notificationResult,
					datasetActiveNotification.jobId,
					datasetActiveNotification.jobType,
					datasetActiveNotification.datasetId,
					datasetActiveNotification.datasetIdentification,
					datasetActiveNotification.datasetName
				)) {
			notifications.add (new StoredNotification (
					t.get (datasetActiveNotification.notificationId), 
					ImportNotificationType.valueOf (t.get (datasetActiveNotification.notificationType)),
					ConfirmNotificationResult.valueOf (t.get (datasetActiveNotification.notificationResult)),
					new JobInfo (
						 t.get (datasetActiveNotification.jobId),
						JobType.valueOf (t.get (datasetActiveNotification.jobType))
					), 
					new BaseDatasetInfo (
						t.get (datasetActiveNotification.datasetIdentification), 
						t.get (datasetActiveNotification.datasetName)
					)
				));
		}
		
		answer (new InfoList<StoredNotification> (notifications, baseQuery.count ()));
						
	}
	

	private void executeGetDataSourceStatus() {
		QJobState jobStateSub = new QJobState("job_state_sub");			
		QHarvestJob harvestJobSub = new QHarvestJob("harvest_job_sub");			
		
		answer(
			DataSourceStatus.class,
				
			query().from(jobState)
				.join(harvestJob).on(harvestJob.jobId.eq(jobState.jobId))
				.rightJoin(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
				.where(isFinished(jobState))
				.where(new SQLSubQuery().from(jobStateSub)
					.join(harvestJobSub).on(harvestJobSub.jobId.eq(jobStateSub.jobId))
					.where(jobStateSub.createTime.after(jobState.createTime))
					.notExists())
				.list(new QDataSourceStatus(
						dataSource.identification, 
						jobState.createTime, 
						jobState.state)));
	}

	private BooleanExpression isFinished(QJobState jobState) {
		return jobState.state.isNull().or(jobState.state.in(enumsToStrings(JobState.getFinished())));
	}

	private void executeUpdateJobState(UpdateJobState query) {
		log.debug("updating job state: " + query);
		
		Integer jobId = getUnfinishedJobQuery( query.getJob())
				.singleResult(job.id);
		
		if(jobId == null) {
			throw new IllegalStateException("job not found");
		}
		
		insert(jobState)
			.set(jobState.jobId, jobId)
			.set(jobState.state, query.getState().name())
			.execute();
		
		ack();
	}

	private SQLQuery getUnfinishedJobQuery(JobInfo job) {		
		if(job instanceof ImportJobInfo) {
			return getUnfinishedJobQuery((ImportJobInfo)job);
		} else if(job instanceof HarvestJobInfo) {
			return getUnfinishedJobQuery((HarvestJobInfo)job);
		} else if(job instanceof ServiceJobInfo) {
			return getUnfinishedJobQuery((ServiceJobInfo)job);		
		} else {
			throw new IllegalArgumentException("unknown job type");
		}
	}
	
	private SQLQuery getLastJobQuery(ImportJobInfo ij) {
		QJob jobSub = new QJob("job_sub");
		QImportJob importJobSub = new QImportJob("import_job_sub");
		QDataset datasetSub = new QDataset("dataset_sub");
		
		return query().from(job)
			.join(importJob).on(importJob.jobId.eq(job.id))
			.join(dataset).on(dataset.id.eq(importJob.datasetId))
			.where(dataset.identification.eq(ij.getDatasetId())
				.and(new SQLSubQuery().from(jobSub)
					.join(importJobSub).on(importJobSub.jobId.eq(jobSub.id))
					.join(datasetSub).on(datasetSub.id.eq(importJobSub.datasetId))
					.where(datasetSub.identification.eq(ij.getDatasetId())
							.and(jobSub.createTime.after(job.createTime)))
					.notExists()));
	}
	
	private SQLQuery getLastJobQuery(HarvestJobInfo hj) {
		QJob jobSub = new QJob("job_sub");
		QHarvestJob harvestJobSub = new QHarvestJob("harvest_job_sub");
		QDataSource dataSourceSub = new QDataSource("data_source_sub");
		
		return query().from(job)
			.join(harvestJob).on(harvestJob.jobId.eq(job.id))
			.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
			.where(dataSource.identification.eq(hj.getDataSourceId())
				.and(new SQLSubQuery().from(jobSub)
					.join(harvestJobSub).on(harvestJobSub.jobId.eq(jobSub.id))
					.join(dataSourceSub).on(dataSourceSub.id.eq(harvestJobSub.dataSourceId))
					.where(dataSourceSub.identification.eq(hj.getDataSourceId())
						.and(jobSub.createTime.after(job.createTime)))
					.notExists()));
	}
	
	private SQLQuery getLastJobQuery(ServiceJobInfo sj) {
		QJob jobSub = new QJob("job_sub");
		QServiceJob serviceJobSub = new QServiceJob("service_job_sub");
		QDataset datasetSub = new QDataset("dataset_sub");
		QSourceDataset sourceDatasetSub = new QSourceDataset("source_dataset_sub");
		QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
		QCategory categorySub = new QCategory("category_sub");
		
		return query().from(job)
				.join(serviceJob).on(serviceJob.jobId.eq(job.id))
				.join(dataset).on(dataset.id.eq(serviceJob.datasetId))
				.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
				.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(serviceJob.sourceDatasetVersionId))
				.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
				.where(dataset.identification.eq(sj.getTableName())
					.and(category.identification.eq(sj.getSchemaName()))
					.and(new SQLSubQuery().from(jobSub)
						.join(serviceJobSub).on(serviceJobSub.jobId.eq(jobSub.id))
						.join(datasetSub).on(datasetSub.id.eq(serviceJobSub.datasetId))
						.join(sourceDatasetSub).on(sourceDatasetSub.id.eq(datasetSub.sourceDatasetId))
						.join(sourceDatasetVersionSub).on(sourceDatasetVersionSub.id.eq(serviceJob.sourceDatasetVersionId))
						.join(categorySub).on(categorySub.id.eq(sourceDatasetVersionSub.categoryId))
						.where(datasetSub.identification.eq(sj.getTableName())
							.and(categorySub.identification.eq(sj.getSchemaName()))
							.and(jobSub.createTime.after(job.createTime)))						
						.notExists()));
				
	}
	
	private SQLQuery getLastJobQuery(JobInfo job) {		
		if(job instanceof ImportJobInfo) {
			return getLastJobQuery((ImportJobInfo)job);
		} else if(job instanceof HarvestJobInfo) {
			return getLastJobQuery((HarvestJobInfo)job);
		} else if(job instanceof ServiceJobInfo) {
			return getLastJobQuery((ServiceJobInfo)job);		
		} else {
			throw new IllegalArgumentException("unknown job type");
		}
	}

	private SQLQuery getUnfinishedJobQuery(ImportJobInfo ij) {
		return query().from(job)
				.join(importJob).on(importJob.jobId.eq(job.id))
				.join(dataset).on(dataset.id.eq(importJob.datasetId))
				.where(dataset.identification.eq(ij.getDatasetId()))
				.where(unfinishedState());
	}

	private BooleanExpression unfinishedState() {
		QJobState jobStateSub = new QJobState("job_state_sub");
		
		return new SQLSubQuery().from(jobStateSub)
				.where(jobStateSub.jobId.eq(job.id)
					.and(isFinished(jobStateSub)))
				.notExists();
	}
	
	private SQLQuery getUnfinishedJobQuery(ServiceJobInfo sj) {
		return query().from(job)
				.join(serviceJob).on(serviceJob.jobId.eq(job.id))
				.join(dataset).on(dataset.id.eq(serviceJob.datasetId))
				.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
				.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(serviceJob.sourceDatasetVersionId))
				.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
				.where(dataset.identification.eq(sj.getTableName()))
				.where(category.identification.eq(sj.getSchemaName()))
				.where(unfinishedState());
	}

	private SQLQuery getUnfinishedJobQuery(HarvestJobInfo hj) {
		return query().from(job)
				.join(harvestJob).on(harvestJob.jobId.eq(job.id))
				.join(dataSource).on(dataSource.id.eq(harvestJob.dataSourceId))
				.where(dataSource.identification.eq(hj.getDataSourceId()))
				.where(unfinishedState());
	}	

	private void executeDeleteDataset(DeleteDataset dds) {
		Long nrOfDatasetColumnsDeleted = delete(datasetColumn)
				.where(
					new SQLSubQuery().from(dataset)
					.where(dataset.identification.eq(dds.getId())
					.and(dataset.id.eq(datasetColumn.datasetId))).exists())
				.execute();
		log.debug("nrOfDatasetColumnsDeleted: " + nrOfDatasetColumnsDeleted);
		
		Long nrOfDatasetsDeleted = delete(dataset)
			.where(dataset.identification.eq(dds.getId()))
			.execute();
		log.debug("nrOfDatasetsDeleted: " + nrOfDatasetsDeleted);
		
		if (nrOfDatasetsDeleted > 0 || nrOfDatasetColumnsDeleted >= 0){
			answer(new Response<Long>(CrudOperation.DELETE, CrudResponse.OK, nrOfDatasetColumnsDeleted));
		} else {
			answer(new Response<String>(CrudOperation.DELETE, CrudResponse.NOK, dds.getId()));
		}
	}

	private void executeUpdatedataset(UpdateDataset uds) {
		String sourceDatasetIdent = uds.getSourceDatasetIdentification();
		String datasetIdent = uds.getDatasetIdentification();
		String datasetName = uds.getDatasetName();
		final String filterConditions = uds.getFilterConditions ();
		log.debug("update dataset" + datasetIdent);
		
		Integer sourceDatasetId = query().from(sourceDataset)
				.where(sourceDataset.identification.eq(sourceDatasetIdent))
				.singleResult(sourceDataset.id);

		update(dataset)
			.set(dataset.name, datasetName)
			.set(dataset.sourceDatasetId, sourceDatasetId)
			.set(dataset.filterConditions, filterConditions)
			.where(dataset.identification.eq(datasetIdent))
			.execute();
			
		Integer datasetId = getDatasetId(datasetIdent);
		
		delete(datasetColumn)
			.where(datasetColumn.datasetId.eq(datasetId))
			.execute();
		
		insertDatasetColumns(datasetId, uds.getColumnList());
		answer(new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, datasetIdent));
		
		log.debug("dataset updated");
	}

	private void executeGetDatasetInfo(GetDatasetInfo gds) {
		String datasetIdent = gds.getId();
		log.debug("get dataset " + datasetIdent);

		final SQLQuery query = query().from(dataset)
			.join (sourceDataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
			.join(sourceDatasetVersion).on(
				sourceDatasetVersion.sourceDatasetId.eq(dataset.sourceDatasetId)
				.and(new SQLSubQuery().from(sourceDatasetVersionSub)
						.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
							.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id)))
						.notExists()))
			.leftJoin (category).on(sourceDatasetVersion.categoryId.eq(category.id))
			.leftJoin (datasetStatus).on (datasetStatus.id.eq (dataset.id))
			.leftJoin (lastImportJob).on (lastImportJob.datasetId.eq (dataset.id))
			.leftJoin (lastServiceJob).on (lastServiceJob.datasetId.eq (dataset.id))
			.leftJoin (datasetActiveNotification).on (datasetActiveNotification.datasetId.eq (dataset.id))
			.where(dataset.identification.eq( datasetIdent ))
			.orderBy (datasetActiveNotification.jobCreateTime.desc ());
		
		final List<StoredNotification> notifications = new ArrayList<> ();
		Tuple lastTuple = null;
		
		for (final Tuple t: query.list (
				dataset.identification, 
				dataset.name, 
				sourceDataset.identification, 
				sourceDatasetVersion.name,
				category.identification,
				category.name, 
				dataset.filterConditions,
				datasetStatus.imported,
				datasetStatus.serviceCreated,
				datasetStatus.sourceDatasetColumnsChanged,
				lastImportJob.finishTime,
				lastImportJob.finishState,
				lastServiceJob.finishTime,
				lastServiceJob.finishState,
				lastServiceJob.verified,
				lastServiceJob.added,
				datasetActiveNotification.notificationId,
				datasetActiveNotification.notificationType,
				datasetActiveNotification.notificationResult,
				datasetActiveNotification.jobId,
				datasetActiveNotification.jobType
			)) {
			
			if (t.get (datasetActiveNotification.notificationId) != null) {
				notifications.add (createStoredNotification (t));
			}
			
			lastTuple = t;
		}
		
		if (lastTuple == null) {
			answer ((Object) null);
		} else {		
			answer (createDatasetInfo (lastTuple, notifications));
		}
	}
	
	private void executeCreateDataset(CreateDataset cds) {
		String sourceDatasetIdent = cds.getSourceDatasetIdentification();
		String datasetIdent = cds.getDatasetIdentification();
		String datasetName = cds.getDatasetName();
		final String filterConditions = cds.getFilterConditions ();
		log.debug("create dataset " + datasetIdent);

		Integer sourceDatasetId = query().from(sourceDataset)
				.where(sourceDataset.identification.eq(sourceDatasetIdent))
				.singleResult(sourceDataset.id);
			if(sourceDatasetId == null) {
				log.error("sourceDataset not found: " + sourceDatasetIdent);
				answer(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, datasetIdent));
			} else {
				insert(dataset)
					.set(dataset.identification, datasetIdent)
					.set(dataset.name, datasetName)
					.set(dataset.sourceDatasetId, sourceDatasetId)
					.set(dataset.filterConditions, filterConditions)
					.execute();
				
				Integer datasetId = getDatasetId(datasetIdent);
				
				insertDatasetColumns(datasetId, cds.getColumnList());					
				answer(new Response<String>(CrudOperation.CREATE, CrudResponse.OK, datasetIdent));
				
				log.debug("dataset inserted");
			}
	}	

	private void executeStoreLog(StoreLog query) throws Exception {
		log.debug("storing log line: " + query);
		
		Integer jobStateId = getLastJobQuery(query.getJob())
			.join(jobState).on(jobState.jobId.eq(job.id))
			.orderBy(jobState.id.desc())
			.limit(1)
			.singleResult(jobState.id);
		
		if(jobStateId == null) {
			throw new IllegalStateException("job not found");
		}
		
		JobLog jl = query.getJobLog();
		
		SQLInsertClause logInsert = insert(jobLog)
			.set(jobLog.jobStateId, jobStateId)
			.set(jobLog.level, jl.getLevel().name())
			.set(jobLog.type, jl.getType().name());
		
		MessageProperties content = jl.getContent();
		if(content == null) {
			logInsert.setNull(jobLog.content);
		} else {		
			logInsert.set(jobLog.content, toJson(jl.getContent()));
		}
		
		logInsert.execute();
		
		ack();
	}
	
	private <T> T fromJson(Class<T> clazz, String json) throws JsonProcessingException, IOException {
		ObjectMapper om = new ObjectMapper();
		return om.reader(clazz).readValue(json);
	}

	private String toJson(Object content) throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		om.setSerializationInclusion(Include.NON_NULL);
		return om.writeValueAsString(content);
	}

	private void executeGetSourceDatasetListInfo(GetSourceDatasetListInfo sdi) {
		log.debug(sdi.toString());
		
		String categoryId = sdi.getCategoryId();
		String dataSourceId = sdi.getDataSourceId();
		String searchStr = sdi.getSearchString();
		
		SQLQuery baseQuery = query().from(sourceDataset)
				.join (sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id)
					.and(new SQLSubQuery().from(sourceDatasetVersionSub)
							.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
									.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id)))
								.notExists()))
				.join (dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.join (category).on(sourceDatasetVersion.categoryId.eq(category.id));
		
		if(categoryId != null) {				
			baseQuery.where(category.identification.eq(categoryId));
		}
		
		if(dataSourceId != null) {				
			baseQuery.where(dataSource.identification.eq(dataSourceId));
		}
		
		if (!(searchStr == null || searchStr.isEmpty())){
			baseQuery.where(sourceDatasetVersion.name.containsIgnoreCase(searchStr)); 				
		}
			
		SQLQuery listQuery = baseQuery.clone()					
				.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));
		
		applyListParams(listQuery, sdi, sourceDatasetVersion.name);
		
		answer(
			new InfoList<SourceDatasetInfo>(			
				listQuery					
					.groupBy(sourceDataset.identification).groupBy(sourceDatasetVersion.name)
					.groupBy(dataSource.identification).groupBy(dataSource.name)
					.groupBy(category.identification).groupBy(category.name)						
					.list(new QSourceDatasetInfo(sourceDataset.identification, sourceDatasetVersion.name, 
							dataSource.identification, dataSource.name,
							category.identification,category.name,
							dataset.count())),
							
				baseQuery.count()
			)
		);
	}

	private void executeGetDataSourceInfo() {
		answer(
			query().from(dataSource)
				.orderBy(dataSource.identification.asc())
				.list(new QDataSourceInfo(dataSource.identification, dataSource.name)));
	}

	private StoredNotification createStoredNotification (final Tuple t) {
		return new StoredNotification (
				t.get (datasetActiveNotification.notificationId), 
				ImportNotificationType.valueOf (t.get (datasetActiveNotification.notificationType)), 
				ConfirmNotificationResult.valueOf (t.get (datasetActiveNotification.notificationResult)), 
				new JobInfo (
					t.get (datasetActiveNotification.jobId), 
					JobType.valueOf (t.get (datasetActiveNotification.jobType))
				), 
				new BaseDatasetInfo (
					t.get (dataset.identification), 
					t.get (dataset.name)
				)
			);
	}
	
	private nl.idgis.publisher.database.messages.DatasetInfo createDatasetInfo (final Tuple t, final List<StoredNotification> notifications) {
		return new nl.idgis.publisher.database.messages.DatasetInfo (
				t.get (dataset.identification), 
				t.get (dataset.name), 
				t.get (sourceDataset.identification), 
				t.get (sourceDatasetVersion.name),
				t.get (category.identification),
				t.get (category.name), 
				t.get (dataset.filterConditions),
				t.get (datasetStatus.imported),
				t.get (datasetStatus.serviceCreated),
				t.get (datasetStatus.sourceDatasetColumnsChanged),
				t.get (lastImportJob.finishTime),
				t.get (lastImportJob.finishState),
				t.get (lastServiceJob.finishTime),
				t.get (lastServiceJob.finishState),
				t.get (lastServiceJob.verified),
				t.get (lastServiceJob.added),
				notifications
			);
	}
	
	private void executeGetDatasetListInfo(GetDatasetListInfo dli) {
		String categoryId = dli.getCategoryId();
		
		SQLQuery baseQuery = query().from(dataset)
			.join (sourceDataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
			.join (sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id)
						.and(new SQLSubQuery().from(sourceDatasetVersionSub)
								.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
										.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id)))
									.notExists()))
			.leftJoin (category).on(sourceDatasetVersion.categoryId.eq(category.id))
			.leftJoin (datasetStatus).on (dataset.id.eq (datasetStatus.id))
			.leftJoin (lastImportJob).on (dataset.id.eq (lastImportJob.datasetId))
			.leftJoin (lastServiceJob).on (dataset.id.eq (lastServiceJob.datasetId))
			.leftJoin (datasetActiveNotification).on (dataset.id.eq (datasetActiveNotification.datasetId));
			
		if(categoryId != null) {
			baseQuery.where(category.identification.eq(categoryId));
		}

		baseQuery
			.orderBy (dataset.identification.asc ())
			.orderBy (datasetActiveNotification.jobCreateTime.desc ());
		
		final List<nl.idgis.publisher.database.messages.DatasetInfo> datasetInfos = new ArrayList<> ();
		String currentIdentification = null;
		final List<StoredNotification> notifications = new ArrayList<> ();
		Tuple lastTuple = null;
		
		for (final Tuple t: baseQuery.list (
				dataset.identification,
				dataset.name,
				sourceDataset.identification,
				sourceDatasetVersion.name,
				category.identification,
				category.name,
				dataset.filterConditions,
				datasetStatus.imported,
				datasetStatus.serviceCreated,
				datasetStatus.sourceDatasetColumnsChanged,
				lastImportJob.finishTime,
				lastImportJob.finishState,
				lastServiceJob.finishTime,
				lastServiceJob.finishState,
				lastServiceJob.verified,
				lastServiceJob.added,
				datasetActiveNotification.notificationId,
				datasetActiveNotification.notificationType,
				datasetActiveNotification.notificationResult,
				datasetActiveNotification.jobId,
				datasetActiveNotification.jobType
			)) {
		
			// Emit a new dataset info:
			final String datasetIdentification = t.get (dataset.identification);
			if (currentIdentification != null && !datasetIdentification.equals (currentIdentification)) {
				datasetInfos.add (createDatasetInfo (lastTuple, notifications));
				notifications.clear ();
			}
			
			// Store the last seen tuple:
			currentIdentification = datasetIdentification; 
			lastTuple = t;
			
			// Add a notification:
			final Integer notificationId = t.get (datasetActiveNotification.notificationId);
			if (notificationId != null) {
				notifications.add (createStoredNotification (t));
			}
		}
		
		if (currentIdentification != null) {
			datasetInfos.add (createDatasetInfo (lastTuple, notifications));
		}

		answer (datasetInfos);
	}

	private void executeGetCategoryListInfo() {
		answer(
				query().from(category)
				.orderBy(category.identification.asc())
				.list(new QCategoryInfo(category.identification,category.name)));
	}

	private void executeRegisterSourceDataset(RegisterSourceDataset rsd) {
		log.debug("registering source dataset: " + rsd);
		
		Dataset dataset = rsd.getDataset();
		Timestamp revision = new Timestamp(dataset.getRevisionDate().getTime());
		Table table = dataset.getTable();
		
		final Integer versionId =
			query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(rsd.getDataSource())
					.and(sourceDataset.identification.eq(dataset.getId())))
				.singleResult(sourceDatasetVersion.id.max());
		
		Integer sourceDatasetId = null;
		if(versionId == null) { // new dataset
			sourceDatasetId = 
				insert(sourceDataset)
					.columns(
						sourceDataset.dataSourceId,
						sourceDataset.identification)
					.select(
						new SQLSubQuery().from(dataSource)
							.list(
								dataSource.id,
								dataset.getId()))
				.executeWithKey(sourceDataset.id);
		} else { // existing dataset
			Tuple existing = 
					query().from(sourceDataset)
						.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
						.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(versionId))
						.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
						.where(dataSource.identification.eq(rsd.getDataSource())
							.and(sourceDataset.identification.eq(dataset.getId())))
						.singleResult(
							sourceDataset.id,
							sourceDatasetVersion.name,
							category.identification,
							sourceDatasetVersion.revision,
							sourceDataset.deleteTime);
			
			sourceDatasetId = existing.get(sourceDataset.id);
			
			String existingName = existing.get(sourceDatasetVersion.name);
			String existingCategoryIdentification = existing.get(category.identification);
			Timestamp existingRevision = existing.get(sourceDatasetVersion.revision);
			Timestamp existingDeleteTime = existing.get(sourceDataset.deleteTime);
			
			List<Column> existingColumns = query().from(sourceDatasetVersionColumn)
					.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(versionId))
					.orderBy(sourceDatasetVersionColumn.index.asc())
					.list(new QColumn(sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.dataType));
			
			if(existingName.equals(table.getName()) // still identical
					&& existingCategoryIdentification.equals(dataset.getCategoryId())
					&& existingRevision.equals(revision)
					&& existingDeleteTime == null
					&& existingColumns.equals(table.getColumns())) {
				
				answer(new AlreadyRegistered());
				return;
			} else {
				if(existingDeleteTime != null) { // reviving dataset
					update(sourceDataset)
						.setNull(sourceDataset.deleteTime)						
						.execute();
				}
			}
		}
		
		int newVersionId = 
			insert(sourceDatasetVersion)
				.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
				.set(sourceDatasetVersion.name, table.getName())
				.set(sourceDatasetVersion.categoryId, getCategoryId(dataset.getCategoryId()))
				.set(sourceDatasetVersion.revision, new Timestamp(dataset.getRevisionDate().getTime()))
				.executeWithKey(sourceDatasetVersion.id);
		
		insertSourceDatasetColumns(newVersionId, table.getColumns());
		
		if(versionId == null) {
			answer(new Registered());
		} else {
			answer(new Updated());
		}
	}

	private void executeGetVersion() {
		log.debug("database version requested");
		
		answer(
			query().from(version)
				.orderBy(version.id.desc())
				.limit(1)
				.singleResult(new QVersion(version.id, version.createTime)));
	}
	
	private void executeStoreNotificationResult (final StoreNotificationResult query) {
		log.debug("storing notification result: " + query);

		if (!query().from (notification)
			.where (notification.id.eq (query.getNotificationId ()))
			.exists ()) {
			return;
		}
		
		if (query ()
			.from (notificationResult)
			.where (notificationResult.notificationId.eq (query.getNotificationId ()))
			.exists ()) {
			
			if (query.getResult ().equals (ConfirmNotificationResult.UNDETERMINED)) {				
				delete (notificationResult)
					.where (notificationResult.notificationId.eq (query.getNotificationId ()))
					.execute ();
			} else {
				update (notificationResult)
					.set (notificationResult.result, query.getResult ().name ())
					.where (notificationResult.notificationId.eq (query.getNotificationId ()))
					.execute ();
			}
		} else {
			if (!query.getResult ().equals (ConfirmNotificationResult.UNDETERMINED)) {
				insert (notificationResult)
					.set (notificationResult.notificationId, query.getNotificationId ())
					.set (notificationResult.result, query.getResult ().name ())
					.execute ();
			}
		}

		answer (new Response<String>(CrudOperation.CREATE, CrudResponse.OK, "" + query.getNotificationId ()));
	}
	
	private void executeGetDatasetColumnDiff (final GetDatasetColumnDiff query) {
		final SQLQuery baseQuery = query ().from (sourceDatasetColumnDiff)
				.join (dataset).on (sourceDatasetColumnDiff.datasetId.eq (dataset.id))
				.where (dataset.identification.eq (query.getDatasetIdentification ()))
				.orderBy (sourceDatasetColumnDiff.name.asc ());
			
		final List<ColumnDiff> diffs = new ArrayList<> ();
		
		for (final Tuple t: baseQuery.clone ().list (sourceDatasetColumnDiff.diff, sourceDatasetColumnDiff.name, sourceDatasetColumnDiff.dataType)) {
			diffs.add (new ColumnDiff (new Column (
					t.get (sourceDatasetColumnDiff.name),
					Type.valueOf (t.get (sourceDatasetColumnDiff.dataType))
				), 
				ColumnDiffOperation.valueOf (t.get (sourceDatasetColumnDiff.diff))
			));
		}
		
		answer (new InfoList<ColumnDiff> (diffs, baseQuery.count ()));
	}
}
