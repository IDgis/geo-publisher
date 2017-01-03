package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QHarvestJob.harvestJob;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import nl.idgis.publisher.admin.messages.QSourceDatasetInfo;
import nl.idgis.publisher.admin.messages.SourceDatasetInfo;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QSourceDataset;
import nl.idgis.publisher.database.QSourceDatasetVersion;
import nl.idgis.publisher.database.QSourceDatasetVersionLog;
import nl.idgis.publisher.database.messages.DataSourceInfo;
import nl.idgis.publisher.database.messages.DatasetInfo;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetDatasetColumnDiff;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetJobLog;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.StoreNotificationResult;
import nl.idgis.publisher.database.messages.StoredJobLog;
import nl.idgis.publisher.database.projections.QColumn;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.MessageType;
import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.query.ListActiveTasks;
import nl.idgis.publisher.domain.query.ListDatasetColumnDiff;
import nl.idgis.publisher.domain.query.ListDatasetColumns;
import nl.idgis.publisher.domain.query.ListIssues;
import nl.idgis.publisher.domain.query.ListSourceDatasetColumns;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.query.PutNotificationResult;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Page.Builder;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.ColumnDiff;
import nl.idgis.publisher.domain.web.ActiveTask;
import nl.idgis.publisher.domain.web.DataSourceStatusType;
import nl.idgis.publisher.domain.web.DatasetImportStatusType;
import nl.idgis.publisher.domain.web.DefaultMessageProperties;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.JobStatusType;
import nl.idgis.publisher.domain.web.Message;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.web.Status;

import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.utils.TypedList;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Expression;
import com.mysema.query.types.ExpressionUtils;
import com.mysema.query.types.Order;
import com.mysema.query.types.query.SimpleSubQuery;

public class Admin extends AbstractAdmin {
	
	private final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvester, loader, provisioning;
	
	public Admin(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef provisioning) {
		super(database);
		
		this.harvester = harvester;
		this.loader = loader;
		this.provisioning = provisioning;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef provisioning) {
		return Props.create(Admin.class, database, harvester, loader, provisioning);
	}
	
	@Override
	protected void preStartAdmin() {
		
		doList(Notification.class, this::handleListDashboardNotifications);
		doList(Issue.class, this::handleListDashboardIssues);
		doQuery(ListActiveTasks.class, this::handleListDashboardActiveTasks);
		
		doGet(SourceDataset.class, this::handleGetSourceDataset);
		
		// TODO: put
		
		// TODO: delete
			
		doQuery(ListIssues.class, this::handleListIssues);
		doQuery(ListSourceDatasetColumns.class, this::handleListSourceDatasetColumns);
		doQuery(ListDatasetColumns.class, this::handleListDatasetColumns);		
		
		doQuery(PutNotificationResult.class, this::handlePutNotificationResult);
		doQuery(ListDatasetColumnDiff.class, this::handleListDatasetColumnDiff);
		doQuery(ListSourceDatasets.class, this::handleListSourceDatasets);
	}
	
	private CompletableFuture<List<Column>> handleListSourceDatasetColumns (final ListSourceDatasetColumns listColumns) {
		log.debug("handleListSourceDatasetColumns");

		return db
				.query()
				.from(sourceDatasetVersionColumn)
				.join(sourceDatasetVersion)
				.on(sourceDatasetVersion.id.eq(sourceDatasetVersionColumn.sourceDatasetVersionId).and(
						new SQLSubQuery()
								.from(sourceDatasetVersionSub)
								.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
										.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id))).notExists()))
				.join(sourceDataset)
				.on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource)
				.on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(sourceDataset.identification.eq(listColumns.getSourceDatasetId()).and(
						dataSource.identification.eq(listColumns.getDataSourceId())))
				.list(new QColumn(
					sourceDatasetVersionColumn.name, 
					sourceDatasetVersionColumn.dataType, 
					sourceDatasetVersionColumn.alias))
				.thenApply(columns -> columns.list());		
	}

	private CompletableFuture<List<Column>> handleListDatasetColumns(final ListDatasetColumns listColumns) {
		log.debug("handleListDatasetColumns");		

		return db.query().from(datasetColumn)
			.join(dataset).on(dataset.id.eq(datasetColumn.datasetId))
			.where(dataset.identification.eq(listColumns.getDatasetId()))
			.list(new QColumn(datasetColumn.name, datasetColumn.dataType, null))
			.thenApply(columns -> columns.list());
	}
	
	@SuppressWarnings("unchecked")
	private CompletableFuture<List<ColumnDiff>> handleListDatasetColumnDiff (final ListDatasetColumnDiff query) {
		return f.ask (database, new GetDatasetColumnDiff (query.datasetIdentification ()), InfoList.class).thenApply(diffs -> {
			return diffs.getList ();
		});
	}
	
	private CompletableFuture<Page<Issue>> handleListDashboardIssues() {
		log.debug ("handleListDashboardIssues");
		
		return handleListIssues (new ListIssues (LogLevel.WARNING.andUp ()));
	}
	
	
	private AsyncSQLQuery recentJobsBaseQuery(Timestamp since){
		return
				db.query().from(job)
				// if there is not jobState (yet), assume the job is planned
				.leftJoin(jobState).on(jobState.jobId.eq(job.id))
				// harvest job and datasource 
				.leftJoin(harvestJob).on(harvestJob.jobId.eq(job.id))
				.leftJoin(dataSource).on(harvestJob.dataSourceId.eq(dataSource.id))
				// import job and dataset 
				.leftJoin(importJob).on(importJob.jobId.eq(job.id))
				.leftJoin(dataset).on(importJob.datasetId.eq(dataset.id))
				// service job and generic layer 
				.leftJoin(serviceJob).on(serviceJob.jobId.eq(job.id))
				.leftJoin(nl.idgis.publisher.database.QService.service).on(serviceJob.serviceId.eq(nl.idgis.publisher.database.QService.service.id))
				.leftJoin(genericLayer).on(nl.idgis.publisher.database.QService.service.genericLayerId.eq(genericLayer.id))			
				.orderBy(jobState.createTime.desc(),job.createTime.desc(),job.type.asc())
				.where(job.createTime.between(since, new Timestamp(new java.util.Date().getTime()))
					.and(jobState.state.isNull().or(jobState.state.ne("STARTED")))
					.and(serviceJob.type.isNull().or(serviceJob.type.ne("VACUUM")))
				);

	}

	private CompletableFuture<TypedList<Tuple>> recentJobsListQuery(AsyncSQLQuery baseQuery, Long page, Long limit){
		AsyncSQLQuery listQuery = baseQuery.clone();
		return listQuery.limit(limit)
			.offset((page - 1) * limit)
			.list(
				job.type,
				job.createTime,
				jobState.state,
				jobState.createTime,
				dataSource.name,
				dataset.name,
				genericLayer.name,
				dataSource.identification,
				dataset.identification,
				genericLayer.identification,
				serviceJob.published
			);
	}
	
private String getEnumName(Enum e){
	return e.getClass().getCanonicalName() + "."  + e.name();
}

	
	private CompletableFuture<List<ActiveTask>> handleRecentJobs(AsyncSQLQuery baseQuery, Long page, Long limit) {
		log.debug("fetching recent jobs");
		return
			recentJobsListQuery(baseQuery, page, limit)
				.thenApply(recentJobs -> {
					List<ActiveTask> recentJobList = new ArrayList<>();
					JobType jt = null ;
					Status js = null;
					
					String identifier = "";
					String title = "";
					Timestamp now = new Timestamp(new java.util.Date().getTime());
					for (Tuple t : recentJobs) {
						if (t.get(job.type).equals("HARVEST")){									
							jt = JobType.HARVEST;
							identifier = t.get (dataSource.identification);
							title = t.get (dataSource.name);
							if (t.get(jobState.state) == null){
								js = new Status(JobStatusType.PLANNED, now);
							} else {
								if (t.get(jobState.state).equals("SUCCEEDED")) {
									js = new Status(DataSourceStatusType.OK, t.get(jobState.createTime));
								} else if (t.get(jobState.state).equals("ABORTED")) {
									js = new Status(DataSourceStatusType.ABORTED, t.get(jobState.createTime));
								} else{
									js = new Status(DataSourceStatusType.FAILED, t.get(jobState.createTime));
								}
							}
						} else if (t.get(job.type).equals("IMPORT")){
							jt = JobType.IMPORT;
							identifier = t.get (dataset.identification);
							title = t.get (dataset.name);
							if (t.get(jobState.state) == null){
								js = new Status(JobStatusType.PLANNED, now);
							} else {
								if (t.get(jobState.state).equals("SUCCEEDED")) {
									js = new Status(DatasetImportStatusType.IMPORTED, t.get(jobState.createTime));
								} else if (t.get(jobState.state).equals("ABORTED")) {
									js = new Status(DatasetImportStatusType.IMPORT_ABORTED, t.get(jobState.createTime));
								} else{
									js = new Status(DatasetImportStatusType.IMPORT_FAILED, t.get(jobState.createTime));
								}
							}
						} else if (t.get(job.type).equals("SERVICE")){
							jt = JobType.SERVICE;
							identifier = t.get (genericLayer.identification);
							title = t.get (genericLayer.name);
							if (t.get(jobState.state) == null){
								js = new Status(JobStatusType.PLANNED, now);
							} else {
								if (t.get(jobState.state).equals("SUCCEEDED")) {
									js = new Status(JobStatusType.OK, t.get(jobState.createTime));
								} else if (t.get(jobState.state).equals("ABORTED")) {
									js = new Status(JobStatusType.ABORTED, t.get(jobState.createTime));
								} else{
									js = new Status(JobStatusType.FAILED, t.get(jobState.createTime));
								}
							}
						}
//						log.debug("\t"+jt + ", " +identifier+ ", " + js );
						recentJobList.add(
							new ActiveTask(
								"", 
								getEnumName(jt), 
								new Message(jt, new DefaultMessageProperties (
									null, 
									identifier, 
									title,
									js.type ())
								), js.since(),
								//active is false because these are past tasks
								false, 
								// published is false if servicejob is for staging, 
								// true for publication, null for none servicejobs
								t.get(serviceJob.published) 
							)
						); 
					}
					return recentJobList;
				});
	}
	
	
	private CompletableFuture<Page<ActiveTask>> handleListDashboardActiveTasks(ListActiveTasks listActiveTasks) {
		log.debug ("handleDashboardActiveTaskList, since=" + listActiveTasks.getSince () + ", page=" + listActiveTasks.getPage () + ", limit=" + listActiveTasks.getLimit ());
		
		CompletableFuture<Object> dataSourceInfo = f.ask(database, new GetDataSourceInfo());
		CompletableFuture<Object> harvestJobs = f.ask(harvester, new GetActiveJobs());
		CompletableFuture<Object> loaderJobs = f.ask(loader, new GetActiveJobs());
		CompletableFuture<Object> serviceJobs = f.ask(provisioning, new GetActiveJobs());
		
		CompletableFuture<Map<String, String>> dataSourceNames = dataSourceInfo.thenApply(msg -> {
			List<DataSourceInfo> dataSourceInfos = (List<DataSourceInfo>)msg;
			
			Map<String, String> retval = new HashMap<String, String>();
			for(DataSourceInfo dsi : dataSourceInfos) {
				retval.put(dsi.getId(), dsi.getName());
			}
			
			return retval;
		});
		
		final CompletableFuture<List<ActiveTask>> activeHarvestTasks = 
			harvestJobs.thenCompose(msg -> {
				final ActiveJobs activeJobs = (ActiveJobs)msg;
				
				return dataSourceNames.thenCompose(dsn -> {
					List<CompletableFuture<ActiveTask>> activeTasks = new ArrayList<>();
					
					for(ActiveJob activeJob : activeJobs.getActiveJobs()) {
						HarvestJobInfo harvestJob = (HarvestJobInfo)activeJob.getJob();
						 
						activeTasks.add(
							f.successful(new ActiveTask(
								"", 
								getEnumName(JobType.HARVEST), 
								new Message(JobType.HARVEST, new DefaultMessageProperties (
									EntityType.DATA_SOURCE, 
									harvestJob.getDataSourceId (),
									dsn.get(harvestJob.getDataSourceId()), 
									JobStatusType.RUNNING)), 
									// active is true because this is a current task
									// published is null because this is not a service job
									new Timestamp(new java.util.Date().getTime()), true, null)));
					}
					
					return f.sequence(activeTasks);					
				});			
		});
		
		final CompletableFuture<List<ActiveTask>> activeDatasetTasks = 
			loaderJobs.thenCompose(msg -> {
					final ActiveJobs activeLoaderJobs = (ActiveJobs)msg;
					
					final Map<String, CompletableFuture<Object>> datasetInfos = new HashMap<>();
					
					return serviceJobs.thenCompose(msg1 -> {
							final ActiveJobs activeServiceJobs = (ActiveJobs)msg1;
							
							List<CompletableFuture<ActiveTask>> activeTasks = new ArrayList<>();
							for(ActiveJob activeLoaderJob : activeLoaderJobs.getActiveJobs()) {
								final ImportJobInfo job = (ImportJobInfo)activeLoaderJob.getJob();
								final Progress progress = (Progress)activeLoaderJob.getProgress();
								
								CompletableFuture<Object> dsi;
								String datasetId = job.getDatasetId();
								if(!datasetInfos.containsKey(datasetId)) {
									// TODO: stop using this database message and start using
									// the stuff in DatasetAdmin.handleListDatasets
									dsi = f.ask(database, new GetDatasetInfo(datasetId));
									
									datasetInfos.put(datasetId, dsi);
								} else {
									dsi = datasetInfos.get(datasetId);
								}
								
								activeTasks.add(dsi.thenApply(msg2 -> {
									DatasetInfo datasetInfo = (DatasetInfo)msg2;
									
									return new ActiveTask(
										"",
										getEnumName(JobType.IMPORT),
										new Message(JobType.IMPORT, new DefaultMessageProperties (
											EntityType.DATASET,
											datasetInfo.getId (),
											datasetInfo.getName (), 
											JobStatusType.RUNNING)),
										// active is true because this is a current task
										// published is null because this is not a service job
										(int)(progress.getCount() * 100 / progress.getTotalCount()), true, null);
								}));
							}
							
							for(ActiveJob activeServiceJob : activeServiceJobs.getActiveJobs()) {								
								final ServiceJobInfo job = (ServiceJobInfo)activeServiceJob.getJob();
								
								activeTasks.add(f.successful(new ActiveTask(
									"", 
									getEnumName(JobType.SERVICE),
									new Message(JobType.SERVICE, new DefaultMessageProperties (
											EntityType.DATASET,
											"",
											"", 
											JobStatusType.RUNNING)),
										// active is true because this is a current task
										// published is not null because this is a service job
									new Timestamp(new java.util.Date().getTime()), true, job.isPublished())));
							}
							
							return f.sequence(activeTasks);
						});					
				});
		
			return activeHarvestTasks.thenCompose(harvestTasks -> {
				return activeDatasetTasks.thenCompose(loaderTasks -> {
					final AsyncSQLQuery jobsBaseQuery = 
						recentJobsBaseQuery(listActiveTasks.getSince());
					return jobsBaseQuery
						.count()
						.thenCompose(count -> {
							return handleRecentJobs(jobsBaseQuery, listActiveTasks.getPage(), listActiveTasks.getLimit())
								.thenApply(recentJobs -> {
									Builder<ActiveTask> builder = new Page.Builder<>();
				
									final Long totalCount = count + harvestTasks.size() + loaderTasks.size();
									// Paging:
									Long limit = listActiveTasks.getLimit();
									long pages = totalCount / limit + Math.min(1, totalCount % limit);
									if(pages > 1) {
										builder
											.setHasMorePages(true)
											.setPageCount(pages)
											.setCurrentPage(listActiveTasks.getPage ());
									} else {
										builder
										.setHasMorePages(false)
										.setPageCount(pages)
										.setCurrentPage(1L);
									}
									builder.addAll(harvestTasks);
									builder.addAll(loaderTasks);
									builder.addAll(recentJobs);
									log.debug(">>> pages: " + pages + ", page=" + builder.getCurrentPage() + ", count=" + totalCount + ", pcount=" + builder.getPageCount() + " <<<");
									return builder.build();
									});				
						});				
			});				
		});
	}

	private CompletableFuture<Page<Notification>> handleListDashboardNotifications() {
		log.debug ("handleDashboardNotificationList");
		 
		final Page.Builder<Notification> dashboardNotifications = new Page.Builder<Notification> ();
		
		return f.successful(dashboardNotifications.build ());
	}
	
	
	
	private CompletableFuture<Optional<SourceDataset>> handleGetSourceDataset(String sourceDatasetId) {
		log.debug("handleSourceDataset");

		AsyncSQLQuery baseQuery = db
				.query()
				.from(sourceDataset)
				.join(sourceDatasetVersion)
				.on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id).and(
						new SQLSubQuery()
								.from(sourceDatasetVersionSub)
								.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
										.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id))).notExists()))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId)).leftJoin(category)
				.on(sourceDatasetVersion.categoryId.eq(category.id));

		if (sourceDatasetId != null) {
			baseQuery.where(sourceDataset.identification.eq(sourceDatasetId));
		}		

		final QSourceDatasetVersion sourceDatasetVersion2 = new QSourceDatasetVersion ("sdv2");
		final QSourceDataset sourceDataset2 = new QSourceDataset ("sd2");
		
		return baseQuery
			.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
			.groupBy(sourceDataset.identification)
			.groupBy(sourceDatasetVersion.id)
			.groupBy(sourceDatasetVersion.name)
			.groupBy(sourceDatasetVersion.alternateTitle)
			.groupBy(dataSource.identification)
			.groupBy(dataSource.name)
			.groupBy(category.identification)
			.groupBy(category.name)
			.groupBy(sourceDataset.deleteTime)
			.groupBy(sourceDatasetVersion.confidential)
			.groupBy(sourceDataset.externalIdentification)
			.singleResult(
				new QSourceDatasetInfo(
						sourceDataset.identification,
						sourceDatasetVersion.name,
						sourceDatasetVersion.alternateTitle,
						dataSource.identification, 
						dataSource.name, 
						category.identification, 
						category.name,
						dataset.count(), 
						new SQLSubQuery ()
							.from (sourceDatasetVersion2)
							.join (sourceDataset2).on (sourceDataset2.id.eq (sourceDatasetVersion2.sourceDatasetId))
							.where (sourceDataset2.identification.eq (sourceDataset.identification))
							.orderBy (sourceDatasetVersion2.createTime.desc ())
							.limit (1)
							.unique (sourceDatasetVersion2.type),
						selectLastLog ("type", sourceDatasetVersion, l -> l.type),
						selectLastLog ("parameters", sourceDatasetVersion, l -> l.content),
						selectLastLog ("time", sourceDatasetVersion, l -> l.createTime),
						sourceDataset.deleteTime,
						sourceDatasetVersion.confidential,
						sourceDataset.externalIdentification
					)).thenApply(sourceDatasetInfoOptional -> 
						sourceDatasetInfoOptional.map(sourceDatasetInfo -> 
							new SourceDataset(
								sourceDatasetInfo.getId(), 
								sourceDatasetInfo.getName(),
								sourceDatasetInfo.getAlternateTitle(),
								sourceDatasetInfo.getCategoryId () == null ? null : new EntityRef(
									EntityType.CATEGORY, 
									sourceDatasetInfo.getCategoryId(), 
									sourceDatasetInfo.getCategoryName()), 
								new EntityRef(
									EntityType.DATA_SOURCE, 
									sourceDatasetInfo.getDataSourceId(), 
									sourceDatasetInfo.getDataSourceName()),
								sourceDatasetInfo.getType (),
								sourceDatasetInfo.getDeleteTime () != null,
								sourceDatasetInfo.isConfidential (),
								sourceDatasetInfo.externalId()
							)));	
	}

	
	private CompletableFuture<Page<Issue>> handleListIssues (final ListIssues listIssues) {
		log.debug("handleListIssues logLevels=" + listIssues.getLogLevels () + ", since=" + listIssues.getSince () + ", page=" + listIssues.getPage () + ", limit=" + listIssues.getLimit ());
		
		final Long page = listIssues.getPage () != null ? Math.max (1, listIssues.getPage ()) : 1;
		final long limit = listIssues.getLimit () != null ? Math.max (1, listIssues.getLimit ()) : DEFAULT_ITEMS_PER_PAGE;
		final long offset = Math.max (0, (page - 1) * limit);
		
		final CompletableFuture<Object> issues = f.ask (database, new GetJobLog (Order.DESC, offset, limit, listIssues.getLogLevels (), listIssues.getSince ()));
		
		return issues.thenApply(msg -> {
			final Page.Builder<Issue> dashboardIssues = new Page.Builder<Issue>();
			
			@SuppressWarnings("unchecked")
			InfoList<StoredJobLog> jobLogs = (InfoList<StoredJobLog>)msg;
			for(StoredJobLog jobLog : jobLogs.getList ()) {
				JobInfo job = jobLog.getJob();
				
				MessageType<?> type = jobLog.getType();
				
				dashboardIssues.add(
					new Issue(
						"" + job.getId(),
						new Message(
							type,
							jobLog.getContent ()
						),
						jobLog.getLevel(),
						job.getJobType(),
						jobLog.getWhen ()
					)
				);
				
			}

			// Paging:
			long count = jobLogs.getCount();
			long pages = count / limit + Math.min(1, count % limit);
			
			if(pages > 1) {
				dashboardIssues
					.setHasMorePages(true)
					.setPageCount(pages)
					.setCurrentPage(page);
			}
			
			return dashboardIssues.build();
		});	
	}
	
	
	
	private CompletableFuture<Response<?>> handlePutNotificationResult (final PutNotificationResult query) {		
		return f.ask (database, 
			new StoreNotificationResult (Integer.parseInt (query.notificationId ()), query.result ()), Response.class)
			.thenApply(resp -> (Response<?>)resp);
	}

	private <RT> SimpleSubQuery<RT> selectLastLog (final String prefix, final QSourceDatasetVersion sourceDatasetVersion, final Function<QSourceDatasetVersionLog, Expression<RT>> resultExpressionBuilder) {		
		final QSourceDatasetVersionLog sdvl = new QSourceDatasetVersionLog (prefix + "_sdvl");
		
		return new SQLSubQuery ()
			.from (sdvl)
			.where (sdvl.sourceDatasetVersionId.eq (sourceDatasetVersion.id))			
			.orderBy (sdvl.createTime.desc ())
			.limit (1)
			.unique (resultExpressionBuilder.apply (sdvl));
	}
	
	private <RT> SimpleSubQuery<RT> selectLastVersion (
			final String prefix, 
			final QSourceDataset sourceDataset, 
			final Function<QSourceDatasetVersion, Expression<RT>> resultExpressionBuilder) {
		
		final QSourceDatasetVersion sourceDatasetVersion2 = new QSourceDatasetVersion (prefix + "_sdv");
		final QSourceDataset sourceDataset2 = new QSourceDataset (prefix + "_sd");
		
		return new SQLSubQuery ()
			.from (sourceDatasetVersion2)
			.join (sourceDataset2).on (sourceDataset2.id.eq (sourceDatasetVersion2.sourceDatasetId))
			.where (sourceDataset2.identification.eq (sourceDataset.identification))
			.orderBy (sourceDatasetVersion2.createTime.desc ())
			.limit (1)
			.unique (resultExpressionBuilder.apply (sourceDatasetVersion2));
	}
	
	private CompletableFuture<Page<SourceDatasetStats>> handleListSourceDatasets(ListSourceDatasets msg) {
		return db.transactional(tx -> {
			AsyncSQLQuery baseQuery = tx.query().from(sourceDataset)
				.join (sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id)
					.and(new SQLSubQuery().from(sourceDatasetVersionSub)
						.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
							.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id)))
						.notExists()))
				.join (dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.leftJoin (category).on(sourceDatasetVersion.categoryId.eq(category.id));
			
			String categoryId = msg.categoryId();
			if(categoryId != null) {				
				baseQuery.where(category.identification.eq(categoryId));
			}
			
			String dataSourceId = msg.dataSourceId();
			if(dataSourceId != null) {				
				baseQuery.where(dataSource.identification.eq(dataSourceId));
			}
			
			String searchStr = msg.getSearchString();
			if (!(searchStr == null || searchStr.isEmpty())){
				baseQuery.where(sourceDatasetVersion.name.containsIgnoreCase(searchStr)); 				
			}
			
			if (msg.getWithErrors () != null) {
				if (msg.getWithErrors ()) {
					baseQuery.where (ExpressionUtils.eqConst (
							selectLastVersion ("db_type", sourceDataset, v -> v.type), 
							"UNAVAILABLE"));
				} else {
					baseQuery.where (ExpressionUtils.neConst (
							selectLastVersion ("db_type", sourceDataset, v -> v.type), 
							"UNAVAILABLE"));
				}
			}
				
			AsyncSQLQuery listQuery = baseQuery.clone()					
				.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));
			
			
			
			Long page = msg.getPage();
			Optional<Long> itemsPerPage = msg.itemsPerPage();			
			singlePage(listQuery, page, itemsPerPage);			
			
			return f
				.collect(listQuery					
					.groupBy(sourceDataset.identification).groupBy(sourceDatasetVersion.name)
					.groupBy(dataSource.identification).groupBy(dataSource.name)
					.groupBy(category.identification).groupBy(category.name)
					.groupBy(sourceDatasetVersion.id)
					.groupBy(sourceDatasetVersion.alternateTitle)
					.groupBy(sourceDataset.deleteTime)
					.groupBy(sourceDatasetVersion.confidential)
					.groupBy(sourceDataset.externalIdentification)
					.orderBy(sourceDatasetVersion.name.trim().asc())
					.list(new QSourceDatasetInfo(
						sourceDataset.identification, 
						sourceDatasetVersion.name,
						sourceDatasetVersion.alternateTitle,
						dataSource.identification, 
						dataSource.name,
						category.identification,
						category.name,
						dataset.count(), 
						sourceDatasetVersion.type,
						selectLastLog ("type", sourceDatasetVersion, l -> l.type),
						selectLastLog ("parameters", sourceDatasetVersion, l -> l.content),
						selectLastLog ("time", sourceDatasetVersion, l -> l.createTime),
						sourceDataset.deleteTime,
						sourceDatasetVersion.confidential,
						sourceDataset.externalIdentification
					)))
				.collect(baseQuery.count()).thenApply((list, count) -> {
					Page.Builder<SourceDatasetStats> pageBuilder = new Page.Builder<> ();
					
					for(SourceDatasetInfo sourceDatasetInfo : list) {
						SourceDataset sourceDataset = new SourceDataset (
							sourceDatasetInfo.getId(), 
							sourceDatasetInfo.getName(),
							sourceDatasetInfo.getAlternateTitle(),
							sourceDatasetInfo.getCategoryId () == null ? null : new EntityRef (EntityType.CATEGORY, sourceDatasetInfo.getCategoryId(),sourceDatasetInfo.getCategoryName()),
							new EntityRef (EntityType.DATA_SOURCE, sourceDatasetInfo.getDataSourceId(), sourceDatasetInfo.getDataSourceName()),
							sourceDatasetInfo.getType (),
							sourceDatasetInfo.getDeleteTime () != null,
							sourceDatasetInfo.isConfidential (),
							sourceDatasetInfo.externalId()
						);
						
						pageBuilder.add (new SourceDatasetStats (
								sourceDataset, 
								sourceDatasetInfo.getCount(),
								sourceDatasetInfo.getLastLogType () == null 
									? null
									: new Message (sourceDatasetInfo.getLastLogType (), sourceDatasetInfo.getLastLogParameters ()),
								sourceDatasetInfo.getLastLogTime ()
							));
					}
					
					addPageInfo(pageBuilder, page, count, itemsPerPage);
					
					return pageBuilder.build();
				});
		});
	}
} 
