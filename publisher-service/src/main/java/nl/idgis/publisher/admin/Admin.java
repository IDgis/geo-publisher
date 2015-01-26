package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetActiveNotification.datasetActiveNotification;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;
import static nl.idgis.publisher.database.QLastServiceJob.lastServiceJob;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.AsyncDatabaseHelper;
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
import nl.idgis.publisher.database.messages.GetSourceDatasetListInfo;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;
import nl.idgis.publisher.database.messages.QSourceDatasetInfo;
import nl.idgis.publisher.database.messages.SourceDatasetInfo;
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
import nl.idgis.publisher.domain.query.GetEntity;
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
import nl.idgis.publisher.domain.web.QCategory;
import nl.idgis.publisher.domain.web.ActiveTask;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DashboardItem;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.DataSourceStatusType;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.DatasetImportStatusType;
import nl.idgis.publisher.domain.web.DatasetServiceStatusType;
import nl.idgis.publisher.domain.web.DefaultMessageProperties;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Issue;
import nl.idgis.publisher.domain.web.Message;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.PutDataset;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.web.Status;

import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Order;

public class Admin extends UntypedActor {
	
	private final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
	
	private final long ITEMS_PER_PAGE = 20;
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader, service, jobSystem;
	
	private final ObjectMapper objectMapper = new ObjectMapper ();
	
	private AsyncDatabaseHelper db;

	private FutureUtils f;
	
	public Admin(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
		this.jobSystem = jobSystem;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, ActorRef jobSystem) {
		return Props.create(Admin.class, database, harvester, loader, service, jobSystem);
	}	
	
	@Override
	public void preStart() throws Exception {
		f = new FutureUtils(getContext().dispatcher(), Timeout.apply(15000));		
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive (final Object message) throws Exception {
		if (message instanceof ListEntity<?>) {
			final ListEntity<?> listEntity = (ListEntity<?>)message;
			
			if (listEntity.cls ().equals (DataSource.class)) {
				handleListDataSources (listEntity);
			} else if (listEntity.cls ().equals (Category.class)) {
				handleListCategories (listEntity);
			} else if (listEntity.cls ().equals (Dataset.class)) {
				handleListDatasets (null);
			} else if (listEntity.cls ().equals (Notification.class)) {
				handleListDashboardNotifications (null);
			} else if (listEntity.cls ().equals (ActiveTask.class)) {
				handleListDashboardActiveTasks (null);
			} else if (listEntity.cls ().equals (Issue.class)) {
				handleListDashboardIssues (null);
			} else {
				handleEmptyList (listEntity);
			}
		} else if (message instanceof GetEntity<?>) {
			final GetEntity<?> getEntity = (GetEntity<?>)message;
			
			if (getEntity.cls ().equals (DataSource.class)) {
				handleGetDataSource (getEntity);
			} else if (getEntity.cls ().equals (Category.class)) {
				handleGetCategory (getEntity);
			} else if (getEntity.cls ().equals (Dataset.class)) {
				handleGetDataset(getEntity);
			} else if (getEntity.cls ().equals (SourceDataset.class)) {
				handleGetSourceDataset(getEntity);
			} else {
				sender ().tell (null, self ());
			}
		} else if (message instanceof PutEntity<?>) {
			final PutEntity<?> putEntity = (PutEntity<?>)message;
			if (putEntity.value() instanceof PutDataset) {
				PutDataset putDataset = (PutDataset)putEntity.value(); 
				if (putDataset.getOperation() == CrudOperation.CREATE){
					handleCreateDataset(putDataset);
				}else{
					handleUpdateDataset(putDataset);
				}
			}
			
		} else if (message instanceof DeleteEntity<?>) {
			final DeleteEntity<?> delEntity = (DeleteEntity<?>)message;
			if (delEntity.cls ().equals (Dataset.class)) {
				handleDeleteDataset(delEntity.id());
			}
		} else if (message instanceof ListSourceDatasets) {
			handleListSourceDatasets ((ListSourceDatasets)message);
		} else if (message instanceof ListDatasets) {
			handleListDatasets (((ListDatasets)message));
		} else if (message instanceof ListSourceDatasetColumns) {
			handleListSourceDatasetColumns ((ListSourceDatasetColumns) message);
		} else if (message instanceof ListDatasetColumns) {
			handleListDatasetColumns ((ListDatasetColumns) message);
		} else if (message instanceof RefreshDataset) {
			handleRefreshDataset(((RefreshDataset) message).getDatasetId());
		} else if (message instanceof ListIssues) {
			handleListIssues ((ListIssues)message);
		} else if (message instanceof ListActiveNotifications) {
			handleListActiveNotifications ((ListActiveNotifications) message);
		} else if (message instanceof PutNotificationResult) {
			handlePutNotificationResult ((PutNotificationResult) message);
		} else if (message instanceof ListDatasetColumnDiff) {
			handleListDatasetColumnDiff ((ListDatasetColumnDiff) message);
		} else {
			unhandled (message);
		}
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
	
	private void handleRefreshDataset(String datasetId) {
		log.debug("requesting to refresh dataset: " + datasetId);
		
		final ActorRef sender = getSender(), self = getSelf();
		Patterns.ask(jobSystem, new CreateImportJob(datasetId), 15000)
			.onComplete(new OnComplete<Object>() {

				@Override
				public void onComplete(Throwable t, Object msg) throws Throwable {
					sender.tell(t == null, self);
				}
				
			}, getContext().dispatcher());
	}
	
	private void handleListSourceDatasetColumns (final ListSourceDatasetColumns listColumns) {
		log.debug("handleListSourceDatasetColumns");
		final ActorRef sender = getSender(), self = getSelf();

		final CompletableFuture<TypedList<Column>> columnList = db
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
				.list(new QColumn(sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.dataType));

		columnList.thenAccept(msg -> {
			log.debug("sourcedataset column list received");
			log.debug("sending sourcedataset column list");
			sender.tell(msg.asCollection(), self);
		});
	}

	private void handleListDatasetColumns(final ListDatasetColumns listColumns) {
		log.debug("handleListDatasetColumns");
		final ActorRef sender = getSender(), self = getSelf();

		final CompletableFuture<TypedList<Column>> columnList = db.query().from(datasetColumn).join(dataset)
				.on(dataset.id.eq(datasetColumn.datasetId))
				.where(dataset.identification.eq(listColumns.getDatasetId()))
				.list(new QColumn(datasetColumn.name, datasetColumn.dataType));

		columnList.thenAccept(msg -> {
			log.debug("dataset column list received");
			log.debug("sending dataset column list");
			sender.tell(msg.asCollection(), self);
		});		

	}
	
	private void handleListDatasetColumnDiff (final ListDatasetColumnDiff query) {
		final ActorRef sender = sender ();
		final ActorRef self = self ();
		
		final Future<Object> result = Patterns.ask (database, new GetDatasetColumnDiff (query.datasetIdentification ()), 15000);
		
		result.onSuccess(new OnSuccess<Object> () {
			@Override
			public void onSuccess (final Object msg) throws Throwable {
				@SuppressWarnings("unchecked")
				final InfoList<ColumnDiff> diffs = (InfoList<ColumnDiff>) msg;
				
				sender.tell (diffs.getList (), self);
			}
		}, context ().dispatcher ());
	}

	private void handleListDataSources (final ListEntity<?> listEntity) {
		log.debug ("List received for: " + listEntity.cls ().getCanonicalName ());
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final CompletableFuture<Object> activeDataSourcesFuture = f.ask(harvester, new GetActiveDataSources(), 15000);
		
		final CompletableFuture<TypedList<DataSourceInfo>> dataSourceInfoFuture =
				db.query().from(dataSource)
				.orderBy(dataSource.identification.asc())
				.list(new QDataSourceInfo(dataSource.identification, dataSource.name));
		
		activeDataSourcesFuture.thenAccept(msg0 -> {
			final Set<String> activeDataSources = (Set<String>)msg0;
			log.debug("active data sources received");
			
			dataSourceInfoFuture.thenAccept(msg1 -> {
					List<DataSourceInfo> dataSourceList = (List<DataSourceInfo>)msg1.asCollection();
					log.debug("data sources info received");
					
					final Page.Builder<DataSource> pageBuilder = new Page.Builder<> ();
					
					for(DataSourceInfo dataSourceInfo : dataSourceList) {
						final String id = dataSourceInfo.getId() ;
						final DataSource dataSourceBuilt = new DataSource (
								id, 
								dataSourceInfo.getName(),
								new Status (activeDataSources.contains(id) 
										? DataSourceStatusType.OK
										: DataSourceStatusType.NOT_CONNECTED, new Timestamp (new Date ().getTime ())));
						
						pageBuilder.add (dataSourceBuilt);
					}
					
					log.debug("sending data source page");
					sender.tell (pageBuilder.build (), self);
				});
		});
	}
	
	private void handleListCategories(final ListEntity<?> listEntity) {
		log.debug("handleCategoryList");
		
		final ActorRef sender = getSender(), self = getSelf();
		final CompletableFuture<TypedList<Category>> categoryList = 
				db.query().from(category)
				.orderBy(category.identification.asc())
				.list(new QCategory(category.identification, category.name));
		
		categoryList.thenAccept(msg -> {
				log.debug("category info received");
				final Page.Builder<Category> pageBuilder = new Page.Builder<Category> ();
				pageBuilder.addAll(msg.asCollection());				
				log.debug("sending category list");
				sender.tell(pageBuilder.build(), self);
			});
	}
	
	private void handleListDashboardIssues(Object object) {
		log.debug ("handleListDashboardIssues");
		
		handleListIssues (new ListIssues (LogLevel.WARNING.andUp ()));
	}

	private void handleListDashboardActiveTasks(Object object) {
		log.debug ("handleDashboardActiveTaskList");
		
		final Future<Object> dataSourceInfo = Patterns.ask(database, new GetDataSourceInfo(), 15000);
		final Future<Object> harvestJobs = Patterns.ask(harvester, new GetActiveJobs(), 15000);
		final Future<Object> loaderJobs = Patterns.ask(loader, new GetActiveJobs(), 15000);
		final Future<Object> serviceJobs = Patterns.ask(service, new GetActiveJobs(), 15000);
		
		final Future<Map<String, String>> dataSourceNames = dataSourceInfo.map(new Mapper<Object, Map<String, String>>() {
			
			@SuppressWarnings("unchecked")
			public Map<String, String> apply(Object msg) {
				List<DataSourceInfo> dataSourceInfos = (List<DataSourceInfo>)msg;
				
				Map<String, String> retval = new HashMap<String, String>();
				for(DataSourceInfo dataSourceInfo : dataSourceInfos) {
					retval.put(dataSourceInfo.getId(), dataSourceInfo.getName());
				}
				
				return retval;
			}
			
		}, getContext().dispatcher());
		
		final Future<Iterable<ActiveTask>> activeHarvestTasks = 
			harvestJobs.flatMap(new Mapper<Object, Future<Iterable<ActiveTask>>>() {

			@Override
			public Future<Iterable<ActiveTask>> apply(Object msg) {
				final ActiveJobs activeJobs = (ActiveJobs)msg;
				
				return dataSourceNames.map(new Mapper<Map<String, String>, Iterable<ActiveTask>>() {
					
					public Iterable<ActiveTask> apply(Map<String, String> dataSourceNames) {
						List<ActiveTask> activeTasks = new ArrayList<>();
						
						for(ActiveJob activeJob : activeJobs.getActiveJobs()) {
							HarvestJobInfo harvestJob = (HarvestJobInfo)activeJob.getJob();
							 
							activeTasks.add(
									new ActiveTask(
											"" + harvestJob.getId(), 
											dataSourceNames.get(harvestJob.getDataSourceId()), 
											new Message(JobType.HARVEST, new DefaultMessageProperties (
													EntityType.DATA_SOURCE, harvestJob.getDataSourceId (), dataSourceNames.get(harvestJob.getDataSourceId()))), 
											null));
						}
						
						return activeTasks;
					}
				}, getContext().dispatcher());
			}
			
		}, getContext().dispatcher());
		
		final Future<Iterable<ActiveTask>> activeDatasetTasks = 
			loaderJobs.flatMap(new Mapper<Object, Future<Iterable<ActiveTask>>>() {
				
				public Future<Iterable<ActiveTask>> apply(Object msg) {
					final ActiveJobs activeLoaderJobs = (ActiveJobs)msg;
					
					final Map<String, Future<Object>> datasetInfos = new HashMap<String, Future<Object>>();
					
					return serviceJobs.flatMap(new Mapper<Object, Future<Iterable<ActiveTask>>>() {
						
						private Future<Object> getDatasetInfo(String datasetId) {
							if(!datasetInfos.containsKey(datasetId)) {
								datasetInfos.put(
										datasetId,							
										Patterns.ask(
												database, 
												new GetDatasetInfo(datasetId), 
												15000));
							}
							
							return datasetInfos.get(datasetId);
						}
						
						public Future<Iterable<ActiveTask>> apply(Object msg) {
							final ActiveJobs activeServiceJobs = (ActiveJobs)msg;
							
							List<Future<ActiveTask>> activeTasks = new ArrayList<>();
							for(ActiveJob activeLoaderJob : activeLoaderJobs.getActiveJobs()) {
								final ImportJobInfo job = (ImportJobInfo)activeLoaderJob.getJob();
								final Progress progress = (Progress)activeLoaderJob.getProgress();								
								
								activeTasks.add(getDatasetInfo(job.getDatasetId()).map(new Mapper<Object, ActiveTask>() {
									
									public ActiveTask apply(Object msg) {
										DatasetInfo datasetInfo = (DatasetInfo)msg;
										
										return new ActiveTask(
											"" + job.getId(),
											datasetInfo.getName(),
											new Message(JobType.IMPORT, new DefaultMessageProperties (
													EntityType.DATASET, datasetInfo.getId (), datasetInfo.getName ())),
											(int)(progress.getCount() * 100 / progress.getTotalCount()));
									}
									
								}, getContext().dispatcher()));
							}
							
							for(ActiveJob activeServiceJob : activeServiceJobs.getActiveJobs()) {								
								final ServiceJobInfo job = (ServiceJobInfo)activeServiceJob.getJob();
								final Progress progress = (Progress)activeServiceJob.getProgress();
								
								activeTasks.add(getDatasetInfo(job.getTableName()).map(new Mapper<Object, ActiveTask>() {
									
									public ActiveTask apply(Object msg) {
										DatasetInfo datasetInfo = (DatasetInfo)msg;
										
										return new ActiveTask(
											"" + job.getId(),
											datasetInfo.getName(),
											new Message(JobType.SERVICE, new DefaultMessageProperties (
													EntityType.DATASET, datasetInfo.getId (), datasetInfo.getName ())),
											(int)(progress.getCount() * 100 / progress.getTotalCount()));
									}
									
								}, getContext().dispatcher()));
							}
							
							return Futures.sequence(activeTasks, getContext().dispatcher());
						}
						
					}, getContext().dispatcher());		
					
				}
				
			}, getContext().dispatcher());
		
		final ActorRef sender = getSender();
		activeHarvestTasks.flatMap(new Mapper<Iterable<ActiveTask>, Future<Iterable<ActiveTask>>>() {
			
			public Future<Iterable<ActiveTask>> apply(final Iterable<ActiveTask> activeHarvestTasks) {
				return activeDatasetTasks.map(new Mapper<Iterable<ActiveTask>, Iterable<ActiveTask>>() {
					
					public Iterable<ActiveTask> apply(final Iterable<ActiveTask> activeLoaderTasks) {
						return Iterables.concat(activeHarvestTasks, activeLoaderTasks);
					}
				}, getContext().dispatcher());
			}
			
		}, getContext().dispatcher()).onSuccess(new OnSuccess<Iterable<ActiveTask>>() {

			@Override
			public void onSuccess(Iterable<ActiveTask> activeTasks) throws Throwable {
				Builder<ActiveTask> builder = new Page.Builder<>();
				for(ActiveTask activeTask : activeTasks) {
					builder.add(activeTask);
				}
				sender.tell(builder.build(), getSelf());				
			}
			
		}, getContext().dispatcher());
	}

	private void handleListDashboardNotifications(Object object) {
		log.debug ("handleDashboardNotificationList");
		
		final ActorRef sender = getSender();
		 
		final Page.Builder<Notification> dashboardNotifications = new Page.Builder<Notification> ();
		
		sender.tell (dashboardNotifications.build (), getSelf());
	}

	private void handleEmptyList (final ListEntity<?> listEntity) {
		final Page.Builder<Category> builder = new Page.Builder<> ();
		
		sender ().tell (builder.build (), self ());
	}
	
	private void handleGetDataSource (final GetEntity<?> getEntity) {
		final DataSource dataSource = new DataSource (getEntity.id (), "DataSource: " + getEntity.id (), new Status (DataSourceStatusType.OK, new Timestamp (new Date ().getTime ())));
		
		sender ().tell (dataSource, self ());
	}
	
	private void handleGetCategory (final GetEntity<?> getEntity) {
		log.debug ("handleCategory");
		
		final ActorRef sender = getSender();
		
		final CompletableFuture<Category> categoryList = db.query().from(category)
				.where(category.identification.eq(getEntity.id()))
				.singleResult(new QCategory(category.identification, category.name));

		categoryList.thenAccept(category -> {
			if (category != null) {
				log.debug("category received");
				log.debug("sending category: " + category);
				sender.tell(category, getSelf());
			} else {
				sender.tell(new NotFound(), getSelf());
			}
		});
	}
	
	private void handleGetDataset (final GetEntity<?> getEntity) {
		log.debug ("handleDataset");
		
		final ActorRef sender = getSender();
		
		final Future<Object> datasetInfo = Patterns.ask(database, new GetDatasetInfo(getEntity.id ()), 15000);
				datasetInfo.onSuccess(new OnSuccess<Object>() {
					@Override
					public void onSuccess(Object msg) throws Throwable {
						if(msg instanceof DatasetInfo) {
							DatasetInfo datasetInfo = (DatasetInfo)msg;
							log.debug("dataset info received");
							final Dataset dataset = createDataset (datasetInfo, new ObjectMapper ());
							log.debug("sending dataset: " + dataset);
							sender.tell (dataset, getSelf());
						} else {
							sender.tell (new NotFound(), getSelf());
						}
					}
				}, getContext().dispatcher());
	}
	
	private void handleGetSourceDataset(final GetEntity<?> getEntity) {
		log.debug("handleSourceDataset");

		final ActorRef sender = getSender();

		String sourceDatasetId = getEntity.id();

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

		AsyncSQLQuery listQuery = baseQuery.clone().leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id));

		final CompletableFuture<SourceDatasetInfo> sourceDatasetInfo = listQuery
				.groupBy(sourceDataset.identification)
				.groupBy(sourceDatasetVersion.name)
				.groupBy(dataSource.identification)
				.groupBy(dataSource.name)
				.groupBy(category.identification)
				.groupBy(category.name)
				.singleResult(
						new QSourceDatasetInfo(sourceDataset.identification, sourceDatasetVersion.name,
								dataSource.identification, dataSource.name, category.identification, category.name,
								dataset.count()));

		sourceDatasetInfo.thenAccept(msg -> {
			if (msg != null) {
				log.debug("sourcedataset info received");
				final SourceDataset sourceDataset = new SourceDataset(msg.getId(), msg.getName(), new EntityRef(
						EntityType.CATEGORY, msg.getCategoryId(), msg.getCategoryName()), new EntityRef(
						EntityType.DATA_SOURCE, msg.getDataSourceId(), msg.getDataSourceName()));
				log.debug("sending sourcedataset: " + sourceDataset);
				sender.tell(sourceDataset, getSelf());
			} else {
				sender.tell(new NotFound(), getSelf());
			}
		});
	}

	private void handleListSourceDatasets (final ListSourceDatasets message) {
		
		log.debug ("handleListSourceDatasets");
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Long page = message.getPage();
		
		final Long offset, limit;		 
		if(page == null) {
			offset = null;
			limit = null;
		} else {
			offset = (page - 1) * ITEMS_PER_PAGE;
			limit = ITEMS_PER_PAGE;
		}
		
		final Future<Object> sourceDatasetInfo = Patterns.ask(database, new GetSourceDatasetListInfo(message.dataSourceId(), message.categoryId(), message.getSearchString(), offset, limit), 15000);
		
				sourceDatasetInfo.onSuccess(new OnSuccess<Object>() {

					@SuppressWarnings("unchecked")
					@Override
					public void onSuccess(Object msg) throws Throwable {
						InfoList<SourceDatasetInfo> sourceDatasetInfoList = (InfoList<SourceDatasetInfo>)msg;
						log.debug("data sources info received");
						
						final Page.Builder<SourceDatasetStats> pageBuilder = new Page.Builder<> ();
						
						for(SourceDatasetInfo sourceDatasetInfo : sourceDatasetInfoList.getList()) {
							final SourceDataset sourceDataset = new SourceDataset (
									sourceDatasetInfo.getId(), 
									sourceDatasetInfo.getName(),
									new EntityRef (EntityType.CATEGORY, sourceDatasetInfo.getCategoryId(),sourceDatasetInfo.getCategoryName()),
									new EntityRef (EntityType.DATA_SOURCE, sourceDatasetInfo.getDataSourceId(), sourceDatasetInfo.getDataSourceName())
							);
							
							pageBuilder.add (new SourceDatasetStats (sourceDataset, sourceDatasetInfo.getCount()));
						}
						
						if(page != null) {
							long count = sourceDatasetInfoList.getCount();
							long pages = count / ITEMS_PER_PAGE + Math.min(1, count % ITEMS_PER_PAGE);
							
							if(pages > 1) {
								pageBuilder
									.setHasMorePages(true)
									.setPageCount(pages)
									.setCurrentPage(page);
							}
						}
						
						
						log.debug("sending data source page");
						sender.tell (pageBuilder.build (), self);
					}
				}, getContext().dispatcher());
		
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
		final Status importStatus, serviceStatus;
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
		
		final DatasetServiceStatusType serviceStatusType;		
		final Timestamp lastServiceTime = datasetInfo.getLastServiceTime () != null
				? datasetInfo.getLastServiceTime ()
				: new Timestamp (new Date ().getTime ());
		
		if (datasetInfo.getServiceCreated() != null && datasetInfo.getServiceCreated()) {			
			if (datasetInfo.isServiceLayerAdded()) {
				serviceStatusType = DatasetServiceStatusType.ADDED;
			} else if (datasetInfo.isServiceLayerVerified()) {
				serviceStatusType = DatasetServiceStatusType.VERIFIED;
			} else {
				serviceStatusType = DatasetServiceStatusType.NOT_VERIFIED;
			}
		} else {
			JobState serviceJobState = datasetInfo.getLastServiceJobState();
			if( serviceJobState == null) {
				serviceStatusType = DatasetServiceStatusType.NOT_VERIFIED;				
			} else {			
				if (datasetInfo.isServiceLayerVerified()) {
					serviceStatusType = DatasetServiceStatusType.ADD_FAILED;
				} else {
					serviceStatusType = DatasetServiceStatusType.VERIFY_FAILED;
				}
			}
		}
		
		serviceStatus = new Status (serviceStatusType, lastServiceTime);
		
		// Add notifications:
		if (datasetInfo.getNotifications () != null && !datasetInfo.getNotifications ().isEmpty ()) {
			for (final StoredNotification sn: datasetInfo.getNotifications ()) {
				notifications.add (createNotification (sn));
			}
		}
		
		return new Dataset (datasetInfo.getId().toString(), datasetInfo.getName(),
				new Category(datasetInfo.getCategoryId(), datasetInfo.getCategoryName()),
				importStatus,
				serviceStatus,
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
	
	private void handleListDatasets (final ListDatasets listDatasets) {
		log.debug ("handleListDatasets: {}", listDatasets);
		
		String categoryId = listDatasets.categoryId();
		long page = listDatasets.getPage();
				
		final ActorRef sender = getSender(), self = getSelf();
		
		db.transactional(tx -> {
			return tx.query().from(dataset)
				.singleResult(dataset.count())
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
						.leftJoin (lastServiceJob).on (dataset.id.eq (lastServiceJob.datasetId))
						.leftJoin (datasetActiveNotification).on (dataset.id.eq (datasetActiveNotification.datasetId));
							
					if(categoryId != null) {
						baseQuery.where(category.identification.eq(categoryId));
					}
					
					return baseQuery
						.orderBy (dataset.identification.asc ())
						.orderBy (datasetActiveNotification.jobCreateTime.desc ())
						.offset (Math.max(0, (page - 1) * ITEMS_PER_PAGE))
						.limit (ITEMS_PER_PAGE)
						.list (
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
								
								return pageBuilder.build ();
							});
				});
		}).whenComplete((msg, t) -> {
			if(t != null) {
				log.error("failed to retrieve information from database: {}" + t);
			} else {
				sender.tell (msg, self);
			}
		});
	}

	private void handleListIssues (final ListIssues listIssues) {
		log.debug("handleListIssues logLevels=" + listIssues.getLogLevels () + ", since=" + listIssues.getSince () + ", page=" + listIssues.getPage () + ", limit=" + listIssues.getLimit ());
		
		final ActorRef sender = sender ();
		final ActorRef self = self ();

		final long page = listIssues.getPage () != null ? Math.max (1, listIssues.getPage ()) : 1;
		final long limit = listIssues.getLimit () != null ? Math.max (1, listIssues.getLimit ()) : ITEMS_PER_PAGE;
		final long offset = Math.max (0, (page - 1) * limit);
		
		final Future<Object> issues = Patterns.ask (database, new GetJobLog (Order.DESC, offset, limit, listIssues.getLogLevels (), listIssues.getSince ()), 15000);
		
		issues.onSuccess (new OnSuccess<Object> () {
			@Override
			public void onSuccess (final Object msg) throws Throwable {
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
				
				sender.tell(dashboardIssues.build(), self);
			}
		}, getContext().dispatcher());
	}
	
	private void handleListActiveNotifications (final ListActiveNotifications listNotifications) {
		final ActorRef sender = sender ();
		final ActorRef self = self ();
		
		final long page = listNotifications.getPage () != null ? Math.max (1, listNotifications.getPage ()) : 1;
		final long limit = listNotifications.getLimit () != null ? Math.max (1, listNotifications.getLimit ()) : ITEMS_PER_PAGE;
		final long offset = Math.max (0, (page - 1) * limit);

		final Future<Object> notifications = Patterns.ask (database, new GetNotifications (Order.DESC, offset, limit, listNotifications.isIncludeRejected (), listNotifications.getSince ()), 15000);
		
		notifications.onSuccess (new OnSuccess<Object> () {
			@Override
			public void onSuccess (final Object msg) throws Throwable {
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
				
				sender.tell (dashboardNotifications.build(), self);
			}
		}, getContext().dispatcher());
	}
	
	private void handlePutNotificationResult (final PutNotificationResult query) {
		final ActorRef sender = sender ();
		final ActorRef self = self ();
		
		final Future<Object> result = Patterns.ask (database, new StoreNotificationResult (Integer.parseInt (query.notificationId ()), query.result ()), 15000);
		
		result.onSuccess (new OnSuccess<Object> () {
			@Override
			public void onSuccess (final Object msg) throws Throwable {
				sender.tell ((Response<?>) msg, self);
			}
		}, context ().dispatcher ());
	}
} 
