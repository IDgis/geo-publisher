package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;

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
import nl.idgis.publisher.domain.web.DefaultMessageProperties;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.Message;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.messages.Progress;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.common.collect.Iterables;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Order;
import com.mysema.query.types.query.SimpleSubQuery;

public class Admin extends AbstractAdmin {
	
	private final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvester, loader, service, jobSystem;
	
	public Admin(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem) {
		super(database);
		
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
		this.jobSystem = jobSystem;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem) {
		return Props.create(Admin.class, database, harvester, loader, service, jobSystem);
	}
	
	@Override
	protected void preStartAdmin() {
		
		doList(Notification.class, this::handleListDashboardNotifications);
		doList(ActiveTask.class, this::handleListDashboardActiveTasks);
		doList(Issue.class, this::handleListDashboardIssues);
		
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
				.list(new QColumn(sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.dataType))
				.thenApply(columns -> columns.list());		
	}

	private CompletableFuture<List<Column>> handleListDatasetColumns(final ListDatasetColumns listColumns) {
		log.debug("handleListDatasetColumns");		

		return db.query().from(datasetColumn)
			.join(dataset).on(dataset.id.eq(datasetColumn.datasetId))
			.where(dataset.identification.eq(listColumns.getDatasetId()))
			.list(new QColumn(datasetColumn.name, datasetColumn.dataType))
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

	private CompletableFuture<Page<ActiveTask>> handleListDashboardActiveTasks() {
		log.debug ("handleDashboardActiveTaskList");
		
		CompletableFuture<Object> dataSourceInfo = f.ask(database, new GetDataSourceInfo());
		CompletableFuture<Object> harvestJobs = f.ask(harvester, new GetActiveJobs());
		CompletableFuture<Object> loaderJobs = f.ask(loader, new GetActiveJobs());
		CompletableFuture<Object> serviceJobs = f.ask(service, new GetActiveJobs());
		
		CompletableFuture<Map<String, String>> dataSourceNames = dataSourceInfo.thenApply(msg -> {
			List<DataSourceInfo> dataSourceInfos = (List<DataSourceInfo>)msg;
			
			Map<String, String> retval = new HashMap<String, String>();
			for(DataSourceInfo dsi : dataSourceInfos) {
				retval.put(dsi.getId(), dsi.getName());
			}
			
			return retval;
		});
		
		
		final CompletableFuture<Iterable<ActiveTask>> activeHarvestTasks = 
			harvestJobs.thenCompose(msg -> {
				final ActiveJobs activeJobs = (ActiveJobs)msg;
				
				return dataSourceNames.thenApply(dsn -> {
					List<ActiveTask> activeTasks = new ArrayList<>();
					
					for(ActiveJob activeJob : activeJobs.getActiveJobs()) {
						HarvestJobInfo harvestJob = (HarvestJobInfo)activeJob.getJob();
						 
						activeTasks.add(
							new ActiveTask(
									"" + harvestJob.getId(), 
									dsn.get(harvestJob.getDataSourceId()), 
									new Message(JobType.HARVEST, new DefaultMessageProperties (
											EntityType.DATA_SOURCE, harvestJob.getDataSourceId (), dsn.get(harvestJob.getDataSourceId()))), 
									null));
					}
					
					return activeTasks;					
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
									dsi = f.ask(database, new GetDatasetInfo(datasetId));
									
									datasetInfos.put(datasetId, dsi);
								} else {
									dsi = datasetInfos.get(datasetId);
								}
								
								activeTasks.add(dsi.thenApply(msg2 -> {
									DatasetInfo datasetInfo = (DatasetInfo)msg2;
									
									return new ActiveTask(
										"" + job.getId(),
										datasetInfo.getName(),
										new Message(JobType.IMPORT, new DefaultMessageProperties (
												EntityType.DATASET, datasetInfo.getId (), datasetInfo.getName ())),
										(int)(progress.getCount() * 100 / progress.getTotalCount()));
								}));
							}
							
							for(ActiveJob activeServiceJob : activeServiceJobs.getActiveJobs()) {								
								final ServiceJobInfo job = (ServiceJobInfo)activeServiceJob.getJob();
								
								activeTasks.add(f.successful(new ActiveTask(
										"" + job.getId(), 
										"", 
										new Message(JobType.SERVICE, null),
										null)));
							}
							
							return f.sequence(activeTasks);
						});					
				});
		
		return activeHarvestTasks.thenCompose(harvestTasks -> {
			return activeDatasetTasks.thenApply(loaderTasks -> {
				Iterable<ActiveTask> tasks = Iterables.concat(harvestTasks, loaderTasks);
				
				Builder<ActiveTask> builder = new Page.Builder<>();
				for(ActiveTask activeTask : tasks) {
					builder.add(activeTask);
				}
				return builder.build();
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
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId)).join(category)
				.on(sourceDatasetVersion.categoryId.eq(category.id));

		if (sourceDatasetId != null) {
			baseQuery.where(sourceDataset.identification.eq(sourceDatasetId));
		}		

		final QSourceDatasetVersion sourceDatasetVersion2 = new QSourceDatasetVersion ("sdv2");
		final QSourceDataset sourceDataset2 = new QSourceDataset ("sd2");
		
		return baseQuery
			.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
			.groupBy(sourceDataset.identification)
			.groupBy(sourceDatasetVersion.name)
			.groupBy(dataSource.identification)
			.groupBy(dataSource.name)
			.groupBy(category.identification)
			.groupBy(category.name)
			.singleResult(
				new QSourceDatasetInfo(
						sourceDataset.identification, 
						sourceDatasetVersion.name,
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
						selectLastLog ("type", sourceDataset, l -> l.type),
						selectLastLog ("parameters", sourceDataset, l -> l.content),
						selectLastLog ("time", sourceDataset, l -> l.createTime)
					)).thenApply(sourceDatasetInfoOptional -> 
						sourceDatasetInfoOptional.map(sourceDatasetInfo -> 
							new SourceDataset(
								sourceDatasetInfo.getId(), 
								sourceDatasetInfo.getName(), 
								new EntityRef(
									EntityType.CATEGORY, 
									sourceDatasetInfo.getCategoryId(), 
									sourceDatasetInfo.getCategoryName()), 
								new EntityRef(
									EntityType.DATA_SOURCE, 
									sourceDatasetInfo.getDataSourceId(), 
									sourceDatasetInfo.getDataSourceName()),
								sourceDatasetInfo.getType ()
							)));	
	}

	
	private CompletableFuture<Page<Issue>> handleListIssues (final ListIssues listIssues) {
		log.debug("handleListIssues logLevels=" + listIssues.getLogLevels () + ", since=" + listIssues.getSince () + ", page=" + listIssues.getPage () + ", limit=" + listIssues.getLimit ());
		
		final Long page = listIssues.getPage () != null ? Math.max (1, listIssues.getPage ()) : 1;
		final long limit = listIssues.getLimit () != null ? Math.max (1, listIssues.getLimit ()) : ITEMS_PER_PAGE;
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

	private <RT> SimpleSubQuery<RT> selectLastLog (final String prefix, final QSourceDataset sourceDataset, final Function<QSourceDatasetVersionLog, Expression<RT>> resultExpressionBuilder) {
		final QSourceDataset sd = new QSourceDataset (prefix + "_sd");
		final QSourceDatasetVersion sdv = new QSourceDatasetVersion (prefix + "_sdv");
		final QSourceDatasetVersionLog sdvl = new QSourceDatasetVersionLog (prefix + "_sdvl");
		
		return new SQLSubQuery ()
			.from (sdv)
			.join (sd).on (sd.id.eq (sdv.sourceDatasetId))
			.leftJoin (sdvl).on (sdv.id.eq (sdvl.sourceDatasetVersionId))
			.where (sd.identification.eq (sourceDataset.identification))
			.orderBy (sdvl.createTime.desc ())
			.limit (1)
			.unique (resultExpressionBuilder.apply (sdvl));
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
				.join (category).on(sourceDatasetVersion.categoryId.eq(category.id));
			
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
				
			AsyncSQLQuery listQuery = baseQuery.clone()					
				.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));
			
			Long page = msg.getPage();
			singlePage(listQuery, page);
			
			final QSourceDatasetVersion sourceDatasetVersion2 = new QSourceDatasetVersion ("sdv2");
			final QSourceDataset sourceDataset2 = new QSourceDataset ("sd2");
			
			return f
				.collect(listQuery					
					.groupBy(sourceDataset.identification).groupBy(sourceDatasetVersion.name)
					.groupBy(dataSource.identification).groupBy(dataSource.name)
					.groupBy(category.identification).groupBy(category.name)		
					.orderBy(sourceDatasetVersion.name.trim().asc())
					.list(new QSourceDatasetInfo(
						sourceDataset.identification, 
						sourceDatasetVersion.name, 
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
						selectLastLog ("type", sourceDataset, l -> l.type),
						selectLastLog ("parameters", sourceDataset, l -> l.content),
						selectLastLog ("time", sourceDataset, l -> l.createTime)
					)))
				.collect(baseQuery.count()).thenApply((list, count) -> {
					Page.Builder<SourceDatasetStats> pageBuilder = new Page.Builder<> ();
					
					for(SourceDatasetInfo sourceDatasetInfo : list) {
						SourceDataset sourceDataset = new SourceDataset (
							sourceDatasetInfo.getId(), 
							sourceDatasetInfo.getName(),
							new EntityRef (EntityType.CATEGORY, sourceDatasetInfo.getCategoryId(),sourceDatasetInfo.getCategoryName()),
							new EntityRef (EntityType.DATA_SOURCE, sourceDatasetInfo.getDataSourceId(), sourceDatasetInfo.getDataSourceName()),
							sourceDatasetInfo.getType ()
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
					
					addPageInfo(pageBuilder, page, count);
					
					return pageBuilder.build();
				});
		});
	}
} 
