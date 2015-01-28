package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetActiveNotification.datasetActiveNotification;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;
import static nl.idgis.publisher.database.QHarvestJob.harvestJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobLog.jobLog;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;
import static nl.idgis.publisher.database.QLastServiceJob.lastServiceJob;
import static nl.idgis.publisher.database.QNotification.notification;
import static nl.idgis.publisher.database.QNotificationResult.notificationResult;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumnDiff.sourceDatasetColumnDiff;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.utils.EnumUtils.enumsToStrings;
import static nl.idgis.publisher.utils.JsonUtils.fromJson;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nl.idgis.publisher.database.messages.AddNotificationResult;
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
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetJobLog;
import nl.idgis.publisher.database.messages.GetNotifications;
import nl.idgis.publisher.database.messages.GetSourceDatasetListInfo;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.ListQuery;
import nl.idgis.publisher.database.messages.PerformDelete;
import nl.idgis.publisher.database.messages.PerformInsert;
import nl.idgis.publisher.database.messages.PerformQuery;
import nl.idgis.publisher.database.messages.PerformUpdate;
import nl.idgis.publisher.database.messages.QCategoryInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.QDataSourceStatus;
import nl.idgis.publisher.database.messages.QDatasetStatusInfo;
import nl.idgis.publisher.database.messages.QSourceDatasetInfo;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.SourceDatasetInfo;
import nl.idgis.publisher.database.messages.StoreNotificationResult;
import nl.idgis.publisher.database.messages.StoredJobLog;
import nl.idgis.publisher.database.messages.StoredNotification;
import nl.idgis.publisher.database.messages.UpdateDataset;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.MessageType;
import nl.idgis.publisher.domain.MessageTypeUtils;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
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
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedList;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.mysema.query.QueryMetadata;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
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
	
	public static Props props(Config config, Connection connection) {
		return Props.create(PublisherTransaction.class, config, connection);
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
	protected Object executeQuery(Query query) throws Exception {
		if(query instanceof GetCategoryListInfo) {
			return executeGetCategoryListInfo();					
		} else if(query instanceof GetDataSourceInfo) {
			return executeGetDataSourceInfo();
		} else if(query instanceof GetSourceDatasetListInfo) {			
			return executeGetSourceDatasetListInfo((GetSourceDatasetListInfo)query);			
		} else if(query instanceof CreateDataset) {
			return executeCreateDataset((CreateDataset)query);
		} else if(query instanceof GetDatasetInfo) {			
			return executeGetDatasetInfo((GetDatasetInfo)query);
		} else if(query instanceof UpdateDataset) {						
			return executeUpdatedataset((UpdateDataset)query);
		} else if(query instanceof DeleteDataset) {
			return executeDeleteDataset((DeleteDataset)query);
		} else if(query instanceof GetDataSourceStatus) {
			return executeGetDataSourceStatus();
		} else if(query instanceof GetJobLog) {
			return executeGetJobLog((GetJobLog)query);
		} else if(query instanceof GetDatasetStatus) {
			return executeGetDatasetStatus((GetDatasetStatus)query);
		} else if(query instanceof AddNotificationResult) {
			return executeAddNotificationResult((AddNotificationResult)query);
		} else if (query instanceof GetNotifications) {
			return executeGetNotifications ((GetNotifications) query);
		} else if (query instanceof StoreNotificationResult) {
			return executeStoreNotificationResult ((StoreNotificationResult) query);
		} else if (query instanceof GetDatasetColumnDiff) {
			return executeGetDatasetColumnDiff ((GetDatasetColumnDiff) query);
		} else if (query instanceof PerformQuery) {
			return executePerformQuery((PerformQuery)query);
		} else if (query instanceof PerformDelete) {
			return executePerformDelete((PerformDelete)query);		
		} else if (query instanceof PerformInsert) {
			return executePerformInsert((PerformInsert)query);
		} else if (query instanceof PerformUpdate) {
			return executePerformUpdate((PerformUpdate)query);
		} else {
			return null;
		}
	}

	private Object executePerformQuery(PerformQuery query) {
		QueryMetadata metadata = query.getMetadata();
		
		List<Expression<?>> projection = metadata.getProjection();
		if(projection.size() == 1) {	
			return toTypedList(metadata, projection.get(0));
		} else {		
			return new TypedList<>(
				Tuple.class,		
				query(metadata).list(projection.toArray(new Expression<?>[projection.size()])));
		}
	}
	
	private Object executePerformUpdate(PerformUpdate query) {
		SQLUpdateClause update = update(query.getEntity());
		
		update.set(query.getColumns(), query.getValues());
		
		QueryMetadata metadata = query.getMetadata();
		if(metadata != null) {
			update.where(metadata.getWhere());
		}
		
		return update.execute();		
	}
	
	private Object executePerformDelete(PerformDelete query) {
		SQLDeleteClause delete = delete(query.getEntity());
		
		return delete.where(query.getMetadata().getWhere()).execute();
	}
	
	private Object executePerformInsert(PerformInsert query) {
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
			return insert.executeWithKey(key);
		} else {
			return insert.execute();
		}
	}	

	private Object executeAddNotificationResult(AddNotificationResult query) {
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
		
		return new Ack();
	}	
	
	private Object executeGetDatasetStatus(GetDatasetStatus query) {		
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
			return new TypedList<>(
				DatasetStatusInfo.class,
				
				baseQuery.list(expression));
		} else {
			return
				baseQuery.where(dataset.identification.eq(datasourceId))
				.singleResult(expression);
		}
	}	

	private Integer getDatasetId(String datasetIdentification) {
		return query().from(dataset)
			.where(dataset.identification.eq(datasetIdentification))
			.singleResult(dataset.id);
	}	

	private Object executeGetJobLog(GetJobLog query) throws Exception {
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
		
		return new InfoList<StoredJobLog> (jobLogs, baseQuery.count ());
	}
	
	private Object executeGetNotifications (final GetNotifications query) throws Exception {
		
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
					(long)t.get (datasetActiveNotification.notificationId), 
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
		
		return new InfoList<StoredNotification> (notifications, baseQuery.count ());
						
	}
	

	private Object executeGetDataSourceStatus() {
		QJobState jobStateSub = new QJobState("job_state_sub");			
		QHarvestJob harvestJobSub = new QHarvestJob("harvest_job_sub");			
		
		return new TypedList<>(
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
	
	private Object executeDeleteDataset(DeleteDataset dds) {
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
			return new Response<Long>(CrudOperation.DELETE, CrudResponse.OK, nrOfDatasetColumnsDeleted);
		} else {
			return new Response<String>(CrudOperation.DELETE, CrudResponse.NOK, dds.getId());
		}
	}

	private Object executeUpdatedataset(UpdateDataset uds) {
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
		
		log.debug("dataset updated");
		
		return new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, datasetIdent);
	}

	private Object executeGetDatasetInfo(GetDatasetInfo gds) {
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
			return null;
		} else {		
			return createDatasetInfo (lastTuple, notifications);
		}
	}
	
	private Object executeCreateDataset(CreateDataset cds) {
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
				return new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, datasetIdent);
			} else {
				insert(dataset)
					.set(dataset.identification, datasetIdent)
					.set(dataset.uuid, UUID.randomUUID().toString())
					.set(dataset.fileUuid, UUID.randomUUID().toString())
					.set(dataset.name, datasetName)
					.set(dataset.sourceDatasetId, sourceDatasetId)
					.set(dataset.filterConditions, filterConditions)
					.execute();
				
				Integer datasetId = getDatasetId(datasetIdent);
				
				insertDatasetColumns(datasetId, cds.getColumnList());
				
				log.debug("dataset inserted");
				
				return new Response<String>(CrudOperation.CREATE, CrudResponse.OK, datasetIdent);
			}
	}	

	private Object executeGetSourceDatasetListInfo(GetSourceDatasetListInfo sdi) {
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
		
		return
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
			);
	}

	private Object executeGetDataSourceInfo() {
		return
			query().from(dataSource)
				.orderBy(dataSource.identification.asc())
				.list(new QDataSourceInfo(dataSource.identification, dataSource.name));
	}

	private StoredNotification createStoredNotification (final Tuple t) {
		return new StoredNotification (
				(long)t.get (datasetActiveNotification.notificationId), 
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

	private Object executeGetCategoryListInfo() {
		return
				query().from(category)
				.orderBy(category.identification.asc())
				.list(new QCategoryInfo(category.identification,category.name));
	}
	
	private Object executeStoreNotificationResult (final StoreNotificationResult query) {
		log.debug("storing notification result: " + query);

		if (!query().from (notification)
			.where (notification.id.eq (query.getNotificationId ()))
			.exists ()) {			
		} else {		
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
		}

		return (new Response<String>(CrudOperation.CREATE, CrudResponse.OK, "" + query.getNotificationId ()));
	}
	
	private Object executeGetDatasetColumnDiff (final GetDatasetColumnDiff query) {
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
		
		return new InfoList<ColumnDiff> (diffs, baseQuery.count ());
	}
}
