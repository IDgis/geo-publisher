package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetActiveNotification.datasetActiveNotification;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;
import static nl.idgis.publisher.database.QHarvestJob.harvestJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobLog.jobLog;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;
import static nl.idgis.publisher.database.QNotification.notification;
import static nl.idgis.publisher.database.QNotificationResult.notificationResult;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumnDiff.sourceDatasetColumnDiff;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.utils.EnumUtils.enumsToStrings;
import static nl.idgis.publisher.utils.JsonUtils.fromJson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import nl.idgis.publisher.database.messages.AddNotificationResult;
import nl.idgis.publisher.database.messages.BaseDatasetInfo;
import nl.idgis.publisher.database.messages.CopyTable;
import nl.idgis.publisher.database.messages.CreateIndices;
import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.CreateView;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.DropTable;
import nl.idgis.publisher.database.messages.DropView;
import nl.idgis.publisher.database.messages.GetCategoryListInfo;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetColumnDiff;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetJobLog;
import nl.idgis.publisher.database.messages.GetNotifications;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.InsertRecords;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.ListQuery;
import nl.idgis.publisher.database.messages.PerformDelete;
import nl.idgis.publisher.database.messages.PerformInsert;
import nl.idgis.publisher.database.messages.PerformInsertBatch;
import nl.idgis.publisher.database.messages.PerformQuery;
import nl.idgis.publisher.database.messages.PerformUpdate;
import nl.idgis.publisher.database.messages.QCategoryInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.QDataSourceStatus;
import nl.idgis.publisher.database.messages.QDatasetStatusInfo;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.ReplaceTable;
import nl.idgis.publisher.database.messages.StoreNotificationResult;
import nl.idgis.publisher.database.messages.StoredJobLog;
import nl.idgis.publisher.database.messages.StoredNotification;
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
import nl.idgis.publisher.provider.protocol.WKBGeometry;
import nl.idgis.publisher.utils.TypedList;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;

import com.mysema.query.QueryMetadata;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Order;
import com.mysema.query.types.Path;
import com.mysema.query.types.SubQueryExpression;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.ComparableExpressionBase;
import com.mysema.query.types.path.PathBuilder;
import com.typesafe.config.Config;

public class PublisherTransaction extends QueryDSLTransaction {
	
	private final static PathBuilder<Long> layerCountPath = new PathBuilder<Long> (Long.class, "layerCount");
	private final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public PublisherTransaction(Config config, Connection connection) {
		super(config, connection);
	}
	
	public static Props props(Config config, Connection connection) {
		return Props.create(PublisherTransaction.class, config, connection);
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
		} else if(query instanceof GetDatasetInfo) {			
			return executeGetDatasetInfo((GetDatasetInfo)query);
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
		} else if (query instanceof CreateTable) {
			return executeCreateTable((CreateTable)query);
		} else if (query instanceof InsertRecords) {
			return executeInsertRecords((InsertRecords)query);
		} else if (query instanceof CreateView) {
			return executeCreateView((CreateView)query);
		} else if (query instanceof CopyTable) {
			return executeCopyTable((CopyTable)query);
		} else if (query instanceof DropView) {
			return executeDropView((DropView)query);
		} else if (query instanceof DropTable) {
			return executeDropTable((DropTable)query);
		} else if (query instanceof CreateIndices) {
			return executeCreateIndices((CreateIndices)query);
		} else if (query instanceof ReplaceTable) {
			return executeReplaceTable((ReplaceTable)query);
		} else {
			return null;
		}
	}	

	private Object executeReplaceTable(ReplaceTable query) throws SQLException {
		String schemaName = query.getSchemaName();
		String fromTable = query.getFromTable();
		String toTable = query.getToTable();
		
		execute("drop table if exists \"" + schemaName + "\".\"" + toTable + "\"");
		execute("alter table \"" + schemaName + "\".\"" + fromTable + "\" rename to \"" + toTable + "\"");
		
		return new Ack();
	}

	private Object executeCreateIndices(CreateIndices query) throws Exception {
		String schemaName = query.getSchemaName();
		String tableName = query.getTableName();
		List<Column> columns = query.getColumns();
		
		for(Column column : columns) {
			String columnName = column.getName();			
			Type dataType = column.getDataType();
			
			String indexMethod;
			if(dataType.equals(Type.GEOMETRY)) {
				indexMethod = "gist";
			} else {
				indexMethod = "btree";
			}
			
			execute("create index \"" + tableName + "_" + columnName + "_idx\" on \"" +
					schemaName + "\".\"" + tableName + "\" using " + indexMethod + "(\"" + columnName + "\")");
		}

		return new Ack();
	}

	private Object executePerformQuery(PerformQuery query) {
		QueryMetadata metadata = query.getMetadata();
		log.debug("executing perform query: {}", query(metadata));
		
		List<Expression<?>> projection = metadata.getProjection();
		
		switch(projection.size()) {
			case 0:
				return query(metadata).count();
			case 1:
				return toTypedList(metadata, projection.get(0));				
			default:
				return new TypedList<>(
					Tuple.class,		
					query(metadata).list(projection.toArray(new Expression<?>[projection.size()])));
		}
	}
	
	private Object executePerformUpdate(PerformUpdate query) {
		SQLUpdateClause update = update(query.getEntity());
		log.debug("executing perform update: {}", update);
		
		update.set(query.getColumns(), query.getValues());
		
		QueryMetadata metadata = query.getMetadata();
		if(metadata != null) {
			update.where(metadata.getWhere());
		}
		
		return update.execute();		
	}
	
	private Object executePerformDelete(PerformDelete query) {
		SQLDeleteClause delete = delete(query.getEntity());
		log.debug("executing perform delete: {}", delete);
		
		return delete.where(query.getMetadata().getWhere()).execute();
	}
	
	private Object executePerformInsert(PerformInsert query) {
		SQLInsertClause insert = insert(query.getEntity());
		log.debug("executing perform insert: {}", insert);
		
		Iterator<PerformInsertBatch> batchItr = query.getBatches().iterator();		
		
		processInsertBatch(insert, batchItr.next());
		
		if(batchItr.hasNext()) {
			insert.addBatch();
			
			while(batchItr.hasNext()) {
				processInsertBatch(insert, batchItr.next());
				insert.addBatch();
			}
		}
		
		Optional<Path<?>> key = query.getKey();
		
		if(key.isPresent()) {			
			return toTypedList(insert, key.get());
		} else {
			return insert.execute();
		}
	}

	private void processInsertBatch(SQLInsertClause insert, PerformInsertBatch batch) {
		Path<?>[] columns = batch.getColumns();
		
		Optional<SubQueryExpression<?>> subQuery = batch.getSubQuery();
		if(subQuery.isPresent()) {
			insert
				.columns(columns)
				.select(subQuery.get());
		} else {
			insert
				.columns(columns)
				.values((Object[])batch.getValues());
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
						Expressions.constant(false), 
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
				Expressions.constant(false),
				datasetStatus.sourceDatasetColumnsChanged,
				lastImportJob.finishTime,
				lastImportJob.finishState,				
				datasetActiveNotification.notificationId,
				datasetActiveNotification.notificationType,
				datasetActiveNotification.notificationResult,
				datasetActiveNotification.jobId,
				datasetActiveNotification.jobType,
				new SQLSubQuery ().from (leafLayer).where (leafLayer.datasetId.eq (dataset.id)).count ().as (layerCountPath),
				sourceDatasetVersion.confidential,
				sourceDatasetVersion.wmsOnly,
				sourceDatasetVersion.archived,
				dataset.metadataFileIdentification
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
				t.get (datasetStatus.sourceDatasetColumnsChanged),
				t.get (lastImportJob.finishTime),
				t.get (lastImportJob.finishState),
				notifications,
				t.get (layerCountPath),
				Long.MAX_VALUE, // dummy value
				t.get (sourceDatasetVersion.confidential),
				t.get (sourceDatasetVersion.wmsOnly),
				t.get (dataset.metadataFileIdentification),
				t.get (sourceDatasetVersion.physicalName),
				t.get (sourceDatasetVersion.archived)
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
					Type.valueOf (t.get (sourceDatasetColumnDiff.dataType)),
					null // alias
				), 
				ColumnDiffOperation.valueOf (t.get (sourceDatasetColumnDiff.diff))
			));
		}
		
		return new InfoList<ColumnDiff> (diffs, baseQuery.count ());
	}
	
	private static class Prepared {
		
		final PreparedStatement stmt;
		
		final int columnCount;
		
		private Prepared(PreparedStatement stmt, int columnCount) {
			this.stmt = stmt;
			this.columnCount = columnCount;
		}
		
		public void batch(List<Object> args, Function<Object, Object> converter) throws Exception {
			if(args.size() != columnCount) {
				throw new RuntimeException("column count doesn't match, expected: " + columnCount + " received: " + args.size()); 
			}
			
			setObjects(args, converter);
			
			stmt.addBatch();
		}
		
		public void executeBatch() throws Exception {			
			stmt.executeBatch();
			stmt.close();
		}

		private void setObjects(List<Object> args, Function<Object, Object> converter) throws Exception {
			int i = 1;
			
			for(Object arg : args) {
				stmt.setObject(i++, converter.apply(arg));
			}
		}
	}
	
	private Prepared prepare(String sql, int columnCount) throws SQLException {
		return new Prepared(connection.prepareStatement(sql), columnCount);
	}
	
	private void execute(String sql) throws SQLException {
		Statement stmt = connection.createStatement();
		stmt.execute(sql);
		stmt.close();
	}
	
	private Object executeInsertRecords(InsertRecords query) throws Exception {
		String schemaName = query.getSchemaName();
		String tableName = query.getTableName();
		List<Column> columns = query.getColumns();
		List<List<Object>> records = query.getRecords();
		
		if(records.isEmpty()) {
			log.warning("trying to insert empty record set");
			
			return new Ack();
		}
		
		StringBuilder sb = new StringBuilder("insert into \"");
		sb.append(schemaName);
		sb.append("\".\"");
		sb.append(tableName);
		sb.append("\"(");
		
		String separator = "";
		for(Column column : columns) {
			sb.append(separator);
			sb.append("\"");
			sb.append(column.getName());
			sb.append("\"");
			
			separator = ", ";
		}
		
		sb.append(") values (");
		
		separator = "";
		for(Column column : columns) {
			sb.append(separator);
			if(column.getDataType() == Type.GEOMETRY) {
				sb.append("ST_SetSRID(ST_Force2D(ST_GeomFromWKB(?)), 28992)");
			} else {
				sb.append("?");
			}
			
			separator = ", ";
		}	
		
		sb.append(")");
		
		String sql = sb.toString();
		log.debug(sql);
		
		Prepared prepared = prepare(sql, columns.size());
		
		for(List<Object> values : records) {
			prepared.batch(values, new Function<Object, Object>() {
	
				@Override
				public Object apply(Object o) throws Exception {
					if(o instanceof WKBGeometry) {
						return ((WKBGeometry) o).getBytes();
					} else {
						return o;
					}
				}
			});
		}
		
		prepared.executeBatch();
		
		log.debug("ack");

		return new Ack();
	}
	
	private Object executeDropView(DropView query) throws Exception {
		String schemaName = query.getSchemaName();
		String viewName = query.getViewName();
		
		// drop view fails if schema doesn't exists
		execute("create schema if not exists \"" + schemaName + "\"");
		
		execute("drop view if exists \"" + schemaName + "\".\"" + viewName + "\"");
		
		return new Ack();
	}
	
	private Object executeDropTable(DropTable query) throws Exception {
		String schemaName = query.getSchemaName();
		String tableName = query.getTableName();
		
		// drop table fails if schema doesn't exists
		execute("create schema if not exists \"" + schemaName + "\"");
		
		execute("drop table if exists \"" + schemaName + "\".\"" + tableName + "\"");
		
		return new Ack();
	}
	
	private Object executeCopyTable(CopyTable query) throws Exception {
		String schemaName = query.getSchemaName();
		String viewName = query.getViewName();
		String sourceSchemaName = query.getSourceSchemaName();
		String sourceTableName = query.getSourceTableName();
		
		execute("create schema if not exists \"" + schemaName + "\"");
		
		execute("create table \"" + schemaName + "\".\"" + viewName + "\" as select * from \"" + sourceSchemaName + "\".\"" + sourceTableName + "\"");
		
		return new Ack();
	}
	
	private Object executeCreateView(CreateView query) throws Exception {
		String schemaName = query.getSchemaName();
		String viewName = query.getViewName();
		String sourceSchemaName = query.getSourceSchemaName();
		String sourceTableName = query.getSourceTableName();
		
		execute("create schema if not exists \"" + schemaName + "\"");
		
		execute("create view \"" + schemaName + "\".\"" + viewName + "\" as select * from \"" + sourceSchemaName + "\".\"" + sourceTableName + "\"");
		
		return new Ack();
	}
	
	private Object executeCreateTable(CreateTable query) throws Exception {
		String schemaName = query.getSchemaName();
		String tableName = query.getTableName();
		List<Column> columns = query.getColumns();
		
		execute("create schema if not exists \"" + schemaName + "\"");
		
		execute("drop table if exists \"" + schemaName + "\".\"" + tableName + "\"");
		
		StringBuilder sb = new StringBuilder("create table \"");
		sb.append(schemaName);
		sb.append("\".\"");
		sb.append(tableName);		
		sb.append("\" (");
		
		String separator = "";
		for(Column column : columns) {
			sb.append(separator);
			sb.append("\"");
			sb.append(column.getName());
			sb.append("\"");
			sb.append(" ");
			
			Type dataType = column.getDataType();
			if(dataType.equals(Type.GEOMETRY)) {
				sb.append("geometry(Geometry, 28992)");
			} else {
				sb.append(dataType.toString().toLowerCase());
			}
			
			separator = ", ";
		}
		
		sb.append(")");
		
		String sql = sb.toString();
		log.debug(sql);
		execute(sql);
		
		log.debug("ack");		
		
		return new Ack();
	}
}
