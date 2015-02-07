package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetActiveNotification.datasetActiveNotification;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QStyle.style;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Order;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import scala.concurrent.Future;

import nl.idgis.publisher.admin.messages.QSourceDatasetInfo;
import nl.idgis.publisher.admin.messages.SourceDatasetInfo;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QSourceDatasetVersion;
import nl.idgis.publisher.database.messages.BaseDatasetInfo;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.DataSourceInfo;
import nl.idgis.publisher.database.messages.DatasetInfo;
import nl.idgis.publisher.database.messages.DeleteDataset;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetDatasetColumnDiff;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetJobLog;
import nl.idgis.publisher.database.messages.GetNotifications;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.StoreNotificationResult;
import nl.idgis.publisher.database.messages.StoredJobLog;
import nl.idgis.publisher.database.messages.StoredNotification;
import nl.idgis.publisher.database.messages.UpdateDataset;
import nl.idgis.publisher.database.projections.QColumn;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.MessageType;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.load.ImportNotificationProperties;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.ListActiveNotifications;
import nl.idgis.publisher.domain.query.ListDatasetColumnDiff;
import nl.idgis.publisher.domain.query.ListDatasetColumns;
import nl.idgis.publisher.domain.query.ListDatasets;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.ListIssues;
import nl.idgis.publisher.domain.query.ListSourceDatasetColumns;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.query.PutNotificationResult;
import nl.idgis.publisher.domain.query.RefreshDataset;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Page.Builder;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.ColumnDiff;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.ActiveTask;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DashboardItem;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.DataSourceStatusType;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.DatasetImportStatusType;
import nl.idgis.publisher.domain.web.DefaultMessageProperties;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.Message;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.PutDataset;
import nl.idgis.publisher.domain.web.PutStyle;
import nl.idgis.publisher.domain.web.QCategory;
import nl.idgis.publisher.domain.web.QStyle;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.web.Status;
import nl.idgis.publisher.domain.web.Style;

import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;

public class Admin extends AbstractAdmin {
	
	private final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvester, loader, service, jobSystem;
	
	private final ObjectMapper objectMapper = new ObjectMapper ();
	
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
	
	protected void unhandledQuery(Object msg) throws Exception {
		if (msg instanceof PutEntity<?>) {
			final PutEntity<?> putEntity = (PutEntity<?>)msg;
			if (putEntity.value() instanceof PutDataset) {
				PutDataset putDataset = (PutDataset)putEntity.value(); 
				if (putDataset.getOperation() == CrudOperation.CREATE){
					handleCreateDataset(putDataset);
				}else{
					handleUpdateDataset(putDataset);
				}
			}
			if (putEntity.value() instanceof PutStyle) {
				PutStyle putStyle = (PutStyle)putEntity.value(); 
				if (putStyle.getOperation() == CrudOperation.CREATE){
					handleCreateStyle(putStyle);
				}else{
//					handleUpdateStyle(putStyle);
				}
			}
		} else if (msg instanceof DeleteEntity<?>) {
			final DeleteEntity<?> delEntity = (DeleteEntity<?>)msg;
			if (delEntity.cls ().equals (Dataset.class)) {
				handleDeleteDataset(delEntity.id());
			} 
		} else {
			log.error("query not handled: {}", msg);
			
			unhandled(msg);
		}
	}
	
	@Override
	public void preStartAdmin() {
		addList(DataSource.class, this::handleListDataSources);
		addList(Category.class, this::handleListCategories);
		addList(Dataset.class, () -> handleListDatasets(null));
		addList(Notification.class, this::handleListDashboardNotifications);
		addList(ActiveTask.class, this::handleListDashboardActiveTasks);
		addList(Issue.class, this::handleListDashboardIssues);
		addList(Style.class, this::handleListStyles);
		
		addGet(Category.class, this::handleGetCategory);
		addGet(DataSource.class, this::handleGetDataSource);
		addGet(Dataset.class, this::handleGetDataset);
		addGet(SourceDataset.class, this::handleGetSourceDataset);
		addGet(Style.class, this::handleGetStyle);
		
		// TODO: put
		
		// TODO: delete
		
		addQuery(ListDatasets.class, this::handleListDatasets);
		addQuery(ListIssues.class, this::handleListIssues);
		addQuery(ListSourceDatasetColumns.class, this::handleListSourceDatasetColumns);
		addQuery(ListDatasetColumns.class, this::handleListDatasetColumns);
		addQuery(RefreshDataset.class, this::handleRefreshDataset);
		addQuery(ListActiveNotifications.class, this::handleListActiveNotifications);
		addQuery(PutNotificationResult.class, this::handlePutNotificationResult);
		addQuery(ListDatasetColumnDiff.class, this::handleListDatasetColumnDiff);
		addQuery(ListSourceDatasets.class, this::handleListSourceDatasets);
	}

	private static Notification createNotification (final StoredNotification storedNotification) {
		return new Notification (
				"" + storedNotification.getId (), 
				new Message (
					storedNotification.getType (), 
					new ImportNotificationProperties (
							EntityType.DATASET, 
							storedNotification.getDataset ().getId (), 
							storedNotification.getDataset ().getName (),
							(ConfirmNotificationResult)storedNotification.getResult ()
						)
				)
			);
	}
	
	private void handleCreateDataset(PutDataset putDataset) throws JsonProcessingException {
		log.debug ("handle create dataset: " + putDataset.id());
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> createDatasetInfo = Patterns.ask(database, 
				new CreateDataset(putDataset.id(), putDataset.getDatasetName(),
				putDataset.getSourceDatasetIdentification(), putDataset.getColumnList(),
				objectMapper.writeValueAsString (putDataset.getFilterConditions())), 15000);
				createDatasetInfo.onSuccess(new OnSuccess<Object>() {
					@Override
					public void onSuccess(Object msg) throws Throwable {
						Response <?> createdDataset = (Response<?>)msg;
						log.debug ("created dataset id: " + createdDataset.getValue());
						sender.tell (createdDataset, self);
					}
				}, getContext().dispatcher());

	}

	private void handleUpdateDataset(PutDataset putDataset) throws JsonProcessingException {
		log.debug ("handle update dataset: " + putDataset.id());
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> updateDatasetInfo = Patterns.ask(database, 
				new UpdateDataset(putDataset.id(), putDataset.getDatasetName(),
				putDataset.getSourceDatasetIdentification(), putDataset.getColumnList(),
				objectMapper.writeValueAsString (putDataset.getFilterConditions ())), 15000);
				updateDatasetInfo.onSuccess(new OnSuccess<Object>() {
					@Override
					public void onSuccess(Object msg) throws Throwable {
						Response<?> updatedDataset = (Response<?>)msg;
						log.debug ("updated dataset id: " + updatedDataset.getValue());
						sender.tell (updatedDataset, self);
					}
				}, getContext().dispatcher());

	}

	private void handleDeleteDataset(String id) {
		log.debug ("handle delete dataset: " + id);
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> deleteDatasetInfo = Patterns.ask(database, new DeleteDataset(id), 15000);
				deleteDatasetInfo.onSuccess(new OnSuccess<Object>() {
					@Override
					public void onSuccess(Object msg) throws Throwable {
						Response<?> deletedDataset = (Response<?>)msg;
						log.debug ("deleted dataset id: " + deletedDataset.getValue());
						sender.tell (deletedDataset, self);
					}
				}, getContext().dispatcher());

	}
	
	private CompletableFuture<Boolean> handleRefreshDataset(RefreshDataset refreshDataset) {
		String datasetId = refreshDataset.getDatasetId();
		
		log.debug("requesting to refresh dataset: {}", datasetId);
		
		return f.ask(jobSystem, new CreateImportJob(datasetId), Ack.class).thenApply(msg -> true);
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
	
	private CompletableFuture<List<ColumnDiff>> handleListDatasetColumnDiff (final ListDatasetColumnDiff query) {
				
		return f.ask (database, new GetDatasetColumnDiff (query.datasetIdentification ())).thenApply(msg -> {
			@SuppressWarnings("unchecked")
			final InfoList<ColumnDiff> diffs = (InfoList<ColumnDiff>) msg;
			
			return diffs.getList ();
		});		
	}

	private CompletableFuture<Page<DataSource>> handleListDataSources () {
		return
			db.query().from(dataSource)
			.orderBy(dataSource.identification.asc())
			.list(new QDataSourceInfo(dataSource.identification, dataSource.name))
			.thenCompose(dataSourceInfos -> 
				f.ask(harvester, new GetActiveDataSources(), Set.class).thenApply(activeDataSources -> {
					final Page.Builder<DataSource> pageBuilder = new Page.Builder<> ();
					
					for(DataSourceInfo dataSourceInfo : dataSourceInfos) {
						final String id = dataSourceInfo.getId() ;
						final DataSource dataSourceBuilt = new DataSource (
							id, 
							dataSourceInfo.getName(),
							new Status (activeDataSources.contains(id) 
								? DataSourceStatusType.OK
								: DataSourceStatusType.NOT_CONNECTED, new Timestamp (new Date ().getTime ())));
						
						pageBuilder.add (dataSourceBuilt);
					}
					
					return pageBuilder.build ();
				}));
	}
	
	private CompletableFuture<Page<Category>> handleListCategories() {
		log.debug("handleCategoryList");
		
		return 
			db.query().from(category)
			.orderBy(category.identification.asc())
			.list(new QCategory(category.identification, category.name))
			.thenApply(this::toPage);
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
		
		final CompletableFuture<Iterable<ActiveTask>> activeDatasetTasks = 
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
								final Progress progress = (Progress)activeServiceJob.getProgress();
								
								activeTasks.add(f.successful(new ActiveTask(
										"" + job.getId(), 
										job.getServiceId(), 
										new Message(JobType.SERVICE, null),
										(int)(progress.getCount() * 100 / progress.getTotalCount()))));
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

	private void handleEmptyList (final ListEntity<?> listEntity) {
		final Page.Builder<Category> builder = new Page.Builder<> ();
		
		sender ().tell (builder.build (), self ());
	}
	
	private CompletableFuture<Optional<DataSource>> handleGetDataSource (String dataSourceId) {
		return f.successful(Optional.of(new DataSource (dataSourceId, "DataSource: " + dataSourceId, new Status (DataSourceStatusType.OK, new Timestamp (new Date ().getTime ())))));
	}
	
	private CompletableFuture<Optional<Category>> handleGetCategory (String categoryId) {
		log.debug ("handleCategory");
		
		return db.query().from(category)
			.where(category.identification.eq(categoryId))
			.singleResult(new QCategory(category.identification, category.name));		
	}
	
	private CompletableFuture<Optional<Dataset>> handleGetDataset (String datasetId) {
		log.debug ("handleDataset");
		
		return f.ask(database, new GetDatasetInfo(datasetId)).thenApply(msg -> {
			try {
				if(msg instanceof DatasetInfo) {
					DatasetInfo datasetInfo = (DatasetInfo)msg;
					log.debug("dataset info received");
					final Dataset dataset = createDataset (datasetInfo, new ObjectMapper ());
					log.debug("sending dataset: " + dataset);
	
					return Optional.of(dataset);
				} else {
					return Optional.empty();
				}
			} catch(Throwable t) {
				throw new RuntimeException(t);
			}
		});				
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

		return baseQuery
			.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
			.groupBy(sourceDataset.identification)
			.groupBy(sourceDatasetVersion.name)
			.groupBy(dataSource.identification)
			.groupBy(dataSource.name)
			.groupBy(category.identification)
			.groupBy(category.name)
			.singleResult(
				new QSourceDatasetInfo(sourceDataset.identification, sourceDatasetVersion.name,
					dataSource.identification, dataSource.name, category.identification, category.name,
					dataset.count())).thenApply(sourceDatasetInfoOptional -> 
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
									sourceDatasetInfo.getDataSourceName()))));	
	}

	
	/*
	 * Admin service Configuration getters
	 */
	
	private CompletableFuture<Optional<Style>> handleGetStyle (String styleId) {
		log.debug ("handleGetStyle");
		
		return 
			db.query().from(style)
			.where(style.identification.eq(styleId))
			.singleResult(new nl.idgis.publisher.domain.web.QStyle(style.identification,style.name,style.format, style.version, style.definition));		
	}
	
	private void handleCreateStyle(PutStyle putStyle) throws JsonProcessingException {
		String styleName = putStyle.getStyle().name();
		log.debug ("handle create style: " + styleName);
		
		final ActorRef sender = getSender();
		
		// Check if there is another style with the same name
		final CompletableFuture<String>  styleId = db.query().from(style)
				.where(style.name.eq(styleName))
				.singleResult(style.name).thenApply(id -> id.get());

		styleId.thenAccept(msg -> {
			if (msg == null){
				// there is no other style with the same name
				log.debug("Inserting new style with name: " + styleName);
				db.insert(style)
				.set(style.identification, UUID.randomUUID().toString())
				.set(style.name, styleName)
				.set(style.version, putStyle.getStyle().version())
				.set(style.format, putStyle.getStyle().format())
				.set(style.definition, putStyle.getStyle().definition())
				.execute();
				
				sender.tell(new Response<String>(CrudOperation.CREATE, CrudResponse.OK, styleName), getSelf());
			} else {
				log.error("Another style found with same name: " + styleName);
				sender.tell(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, styleName), getSelf());
			}
		});
				
	}

	
	/*
	 * Admin service Configuration list
	 */

	private CompletableFuture<Page<Style>> handleListStyles () {
		log.debug ("handleListStyles");
		
		return 
			db.query().from(style)
			.list(new QStyle(style.identification,style.name,style.format, style.version, style.definition))
			.thenApply(this::toPage);
	}
	
	private static DatasetImportStatusType jobStateToDatasetStatus (final JobState jobState) {
		switch (jobState) {
		default:
		case ABORTED:
		case FAILED:
			return DatasetImportStatusType.IMPORT_FAILED;
		case STARTED:
			return DatasetImportStatusType.IMPORTING;
		case SUCCEEDED:
			return DatasetImportStatusType.IMPORTED;
		}
	}

	private static Dataset createDataset (final DatasetInfo datasetInfo, final ObjectMapper objectMapper) throws Throwable {
		// Determine dataset status and notification list:
		final Status importStatus;
		final List<DashboardItem> notifications = new ArrayList<> ();
		if (datasetInfo.getImported () != null && datasetInfo.getImported ()) {
			// Set imported status:
			if (datasetInfo.getLastImportJobState () != null) {
				importStatus = new Status (
						jobStateToDatasetStatus (datasetInfo.getLastImportJobState ()),
						datasetInfo.getLastImportTime () != null
							? datasetInfo.getLastImportTime ()
							: new Timestamp (new Date ().getTime ())
					);
			} else {
				importStatus = new Status (DatasetImportStatusType.NOT_IMPORTED, new Timestamp (new Date ().getTime ()));
			}
		} else {
			// Dataset has never been imported, don't report any notifications:
			importStatus = new Status (DatasetImportStatusType.NOT_IMPORTED, new Timestamp (new Date ().getTime ()));
		}
		
		// Add notifications:
		if (datasetInfo.getNotifications () != null && !datasetInfo.getNotifications ().isEmpty ()) {
			for (final StoredNotification sn: datasetInfo.getNotifications ()) {
				notifications.add (createNotification (sn));
			}
		}
		
		return new Dataset (datasetInfo.getId().toString(), datasetInfo.getName(),
				new Category(datasetInfo.getCategoryId(), datasetInfo.getCategoryName()),
				importStatus,
				notifications, // notification list
				new EntityRef (EntityType.SOURCE_DATASET, datasetInfo.getSourceDatasetId(), datasetInfo.getSourceDatasetName()),
				objectMapper.readValue (datasetInfo.getFilterConditions (), Filter.class)
		);
	}
	
	private static DatasetInfo createDatasetInfo (final Tuple t, final List<StoredNotification> notifications) {
		return new DatasetInfo (
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
				notifications
			);
	}
	
	private static StoredNotification createStoredNotification (final Tuple t) {
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
	
	private CompletableFuture<Page<Dataset>> handleListDatasets (final ListDatasets listDatasets) {
		log.debug ("handleListDatasets: {}", listDatasets);
		
		String categoryId = listDatasets.categoryId();
		long page = listDatasets.getPage();
		
		return db.transactional(tx -> {
			return tx.query().from(dataset)
				.count()
				.thenCompose(datasetCount -> {
					AsyncSQLQuery baseQuery = tx.query().from(dataset)
						.join (sourceDataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
						.join (sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id)
							.and(new SQLSubQuery().from(sourceDatasetVersionSub)
								.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)
									.and(sourceDatasetVersionSub.id.gt(sourceDatasetVersion.id)))
								.notExists()))
						.leftJoin (category).on(sourceDatasetVersion.categoryId.eq(category.id))
						.leftJoin (datasetStatus).on (dataset.id.eq (datasetStatus.id))
						.leftJoin (lastImportJob).on (dataset.id.eq (lastImportJob.datasetId))						
						.leftJoin (datasetActiveNotification).on (dataset.id.eq (datasetActiveNotification.datasetId));
							
					if(categoryId != null) {
						baseQuery.where(category.identification.eq(categoryId));
					}
					
					singlePage(baseQuery, page);
					
					return baseQuery
						.orderBy (dataset.identification.asc ())
						.orderBy (datasetActiveNotification.jobCreateTime.desc ())
						.list (
							dataset.identification,
							dataset.name,
							sourceDataset.identification,
							sourceDatasetVersion.name,
							category.identification,
							category.name,
							dataset.filterConditions,
							datasetStatus.imported,
							datasetStatus.sourceDatasetColumnsChanged,
							lastImportJob.finishTime,
							lastImportJob.finishState,							
							datasetActiveNotification.notificationId,
							datasetActiveNotification.notificationType,
							datasetActiveNotification.notificationResult,
							datasetActiveNotification.jobId,
							datasetActiveNotification.jobType).thenApply(tuples -> {
								final List<DatasetInfo> datasetInfos = new ArrayList<> ();
								String currentIdentification = null;
								final List<StoredNotification> notifications = new ArrayList<> ();
								Tuple lastTuple = null;
								
								for (final Tuple t: tuples) {				
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
								
								log.debug("dataset info received");
								
								final Page.Builder<Dataset> pageBuilder = new Page.Builder<> ();
								final ObjectMapper objectMapper = new ObjectMapper ();
								
								for(DatasetInfo datasetInfo : datasetInfos) {
									try {
										pageBuilder.add (createDataset (datasetInfo, objectMapper));
									} catch(Throwable t) {
										log.error("couldn't create dataset info: {}", t);
									}
								}
								
								log.debug("sending dataset page");
								
								addPageInfo(pageBuilder, page, datasetCount);
								
								return pageBuilder.build ();
							});
				});
		});
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
	
	private CompletableFuture<Page<Notification>> handleListActiveNotifications (final ListActiveNotifications listNotifications) {
		final long page = listNotifications.getPage () != null ? Math.max (1, listNotifications.getPage ()) : 1;
		final long limit = listNotifications.getLimit () != null ? Math.max (1, listNotifications.getLimit ()) : ITEMS_PER_PAGE;
		final long offset = Math.max (0, (page - 1) * limit);

		final CompletableFuture<Object> notifications = f.ask (database, new GetNotifications (Order.DESC, offset, limit, listNotifications.isIncludeRejected (), listNotifications.getSince ()));
		
		return notifications.thenApply(msg -> {
			final Page.Builder<Notification> dashboardNotifications = new Page.Builder<Notification>();
			
			@SuppressWarnings("unchecked")
			final InfoList<StoredNotification> storedNotifications = (InfoList<StoredNotification>)msg;
			
			for (final StoredNotification storedNotification: storedNotifications.getList ()) {
				dashboardNotifications.add (createNotification (storedNotification));
			}
			
			// Paging:
			long count = storedNotifications.getCount ();
			long pages = count / limit + Math.min(1, count % limit);
			
			if(pages > 1) {
				dashboardNotifications
					.setHasMorePages(true)
					.setPageCount(pages)
					.setCurrentPage(page);
			}
			
			return dashboardNotifications.build();
		});		
	}
	
	private CompletableFuture<Response<?>> handlePutNotificationResult (final PutNotificationResult query) {		
		return f.ask (database, 
			new StoreNotificationResult (Integer.parseInt (query.notificationId ()), query.result ()), Response.class)
			.thenApply(resp -> (Response<?>)resp);
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
			
			return f
				.collect(listQuery					
					.groupBy(sourceDataset.identification).groupBy(sourceDatasetVersion.name)
					.groupBy(dataSource.identification).groupBy(dataSource.name)
					.groupBy(category.identification).groupBy(category.name)		
					.orderBy(sourceDatasetVersion.name.trim().asc())
					.list(new QSourceDatasetInfo(sourceDataset.identification, sourceDatasetVersion.name, 
						dataSource.identification, dataSource.name,
						category.identification,category.name,
						dataset.count())))
				.collect(baseQuery.count()).thenApply((list, count) -> {
					Page.Builder<SourceDatasetStats> pageBuilder = new Page.Builder<> ();
					
					for(SourceDatasetInfo sourceDatasetInfo : list) {
						SourceDataset sourceDataset = new SourceDataset (
							sourceDatasetInfo.getId(), 
							sourceDatasetInfo.getName(),
							new EntityRef (EntityType.CATEGORY, sourceDatasetInfo.getCategoryId(),sourceDatasetInfo.getCategoryName()),
							new EntityRef (EntityType.DATA_SOURCE, sourceDatasetInfo.getDataSourceId(), sourceDatasetInfo.getDataSourceName())
						);
						
						pageBuilder.add (new SourceDatasetStats (sourceDataset, sourceDatasetInfo.getCount()));
					}
					
					addPageInfo(pageBuilder, page, count);
					
					return pageBuilder.build();
				});
		});
	}
} 
