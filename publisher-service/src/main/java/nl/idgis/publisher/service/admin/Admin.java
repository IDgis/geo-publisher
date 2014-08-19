package nl.idgis.publisher.service.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.idgis.publisher.database.messages.CategoryInfo;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.DataSourceInfo;
import nl.idgis.publisher.database.messages.DatasetInfo;
import nl.idgis.publisher.database.messages.DeleteDataset;
import nl.idgis.publisher.database.messages.GetCategoryInfo;
import nl.idgis.publisher.database.messages.GetCategoryListInfo;
import nl.idgis.publisher.database.messages.GetDataSourceInfo;
import nl.idgis.publisher.database.messages.GetDatasetColumns;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetDatasetListInfo;
import nl.idgis.publisher.database.messages.GetSourceDatasetColumns;
import nl.idgis.publisher.database.messages.GetSourceDatasetInfo;
import nl.idgis.publisher.database.messages.GetSourceDatasetListInfo;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.SourceDatasetInfo;
import nl.idgis.publisher.database.messages.UpdateDataset;
import nl.idgis.publisher.domain.JobStateType;
import nl.idgis.publisher.domain.JobType;

import nl.idgis.publisher.domain.query.DeleteEntity;
import nl.idgis.publisher.domain.query.GetEntity;
import nl.idgis.publisher.domain.query.ListDatasetColumns;
import nl.idgis.publisher.domain.query.ListDatasets;
import nl.idgis.publisher.domain.query.ListEntity;
import nl.idgis.publisher.domain.query.ListSourceDatasetColumns;
import nl.idgis.publisher.domain.query.ListSourceDatasets;
import nl.idgis.publisher.domain.query.PutEntity;
import nl.idgis.publisher.domain.query.RefreshDataset;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DashboardActiveTask;
import nl.idgis.publisher.domain.web.DashboardActiveTaskType;
import nl.idgis.publisher.domain.web.DashboardError;
import nl.idgis.publisher.domain.web.DashboardErrorType;
import nl.idgis.publisher.domain.web.DashboardNotification;
import nl.idgis.publisher.domain.web.DashboardNotificationType;
import nl.idgis.publisher.domain.web.DataSource;
import nl.idgis.publisher.domain.web.DataSourceStatusType;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.EntityType;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.Message;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.PutDataset;
import nl.idgis.publisher.domain.web.SourceDataset;
import nl.idgis.publisher.domain.web.SourceDatasetStats;
import nl.idgis.publisher.domain.web.Status;
import nl.idgis.publisher.service.harvester.messages.GetActiveDataSources;

import org.joda.time.LocalDateTime;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Admin extends UntypedActor {
	
	private final long ITEMS_PER_PAGE = 20;
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader;
	
	public Admin(ActorRef database, ActorRef harvester, ActorRef loader) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader) {
		return Props.create(Admin.class, database, harvester, loader);
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
			} else if (listEntity.cls ().equals (DashboardNotification.class)) {
				handleListDashboardNotifications (null);
			} else if (listEntity.cls ().equals (DashboardActiveTask.class)) {
				handleListDashboardActiveTasks (null);
			} else if (listEntity.cls ().equals (DashboardError.class)) {
				handleListDashboardErrors (null);
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
		} else {
			unhandled (message);
		}
	}
	
	private void handleCreateDataset(PutDataset putDataset) {
		log.debug ("handle create dataset: " + putDataset.id());
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> createDatasetInfo = Patterns.ask(database, 
				new CreateDataset(putDataset.id(), putDataset.getDatasetName(),
				putDataset.getSourceDatasetIdentification(), putDataset.getColumnList()), 15000);
				createDatasetInfo.onSuccess(new OnSuccess<Object>() {
					@Override
					public void onSuccess(Object msg) throws Throwable {
						Response <?> createdDataset = (Response<?>)msg;
						log.debug ("created dataset id: " + createdDataset.getValue());
						sender.tell (createdDataset, self);
					}
				}, getContext().dispatcher());

	}

	private void handleUpdateDataset(PutDataset putDataset) {
		log.debug ("handle update dataset: " + putDataset.id());
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> updateDatasetInfo = Patterns.ask(database, 
				new UpdateDataset(putDataset.id(), putDataset.getDatasetName(),
				putDataset.getSourceDatasetIdentification(), putDataset.getColumnList()), 15000);
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
		Patterns.ask(database, new CreateImportJob(datasetId), 15000)
			.onComplete(new OnComplete<Object>() {

				@Override
				public void onComplete(Throwable t, Object msg) throws Throwable {
					sender.tell(t == null, self);
				}
				
			}, getContext().dispatcher());
	}
	
	private void handleListSourceDatasetColumns (final ListSourceDatasetColumns listColumns) {
		GetSourceDatasetColumns di = new GetSourceDatasetColumns(listColumns.getDataSourceId(), listColumns.getSourceDatasetId());
		
		database.tell(di, getSender());
	}

	private void handleListDatasetColumns (final ListDatasetColumns listColumns) {
		GetDatasetColumns di = new GetDatasetColumns(listColumns.getDatasetId());
		
		database.tell(di, getSender());
	}

	private void handleListDataSources (final ListEntity<?> listEntity) {
		log.debug ("List received for: " + listEntity.cls ().getCanonicalName ());
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> activeDataSources = Patterns.ask(harvester, new GetActiveDataSources(), 15000);
		final Future<Object> dataSourceInfo = Patterns.ask(database, new GetDataSourceInfo(), 15000);
		
		activeDataSources.onSuccess(new OnSuccess<Object>() {
			
			@Override
			@SuppressWarnings("unchecked")
			public void onSuccess(Object msg) throws Throwable {
				final Set<String> activeDataSources = (Set<String>)msg;
				log.debug("active data sources received");
				
				dataSourceInfo.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						List<DataSourceInfo> dataSourceInfoList = (List<DataSourceInfo>)msg;
						log.debug("data sources info received");
						
						final Page.Builder<DataSource> pageBuilder = new Page.Builder<> ();
						
						for(DataSourceInfo dataSourceInfo : dataSourceInfoList) {
							final String id = dataSourceInfo.getId();
							final DataSource dataSource = new DataSource (
									id, 
									dataSourceInfo.getName(),
									new Status (activeDataSources.contains(id) 
											? DataSourceStatusType.OK
											: DataSourceStatusType.NOT_CONNECTED, LocalDateTime.now ()));
							
							pageBuilder.add (dataSource);
						}
						
						log.debug("sending data source page");
						sender.tell (pageBuilder.build (), self);
					}
				}, getContext().dispatcher());
			}			
		}, getContext().dispatcher());
	}
	
	private void handleListCategories (final ListEntity<?> listEntity) {
		log.debug ("handleCategoryList");
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> categoryListInfo = Patterns.ask(database, new GetCategoryListInfo(), 15000);
				categoryListInfo.onSuccess(new OnSuccess<Object>() {
					@SuppressWarnings("unchecked")
					@Override
					public void onSuccess(Object msg) throws Throwable {
						List<CategoryInfo> categoryListInfoList = (List<CategoryInfo>)msg;
						log.debug("data sources info received");
						
						final Page.Builder<Category> pageBuilder = new Page.Builder<> ();
						
						for(CategoryInfo categoryInfo : categoryListInfoList) {
							pageBuilder.add (new Category (categoryInfo.getId(), categoryInfo.getName()));
						}
						
						log.debug("sending category list");
						sender.tell (pageBuilder.build (), self);
					}
				}, getContext().dispatcher());
	}
	
	private void handleListDashboardErrors(Object object) {
		log.debug ("handleDashboardErrorList");
		
		final ActorRef sender = getSender(), self = getSelf();
		
		// TODO get content from joblog
		final Page.Builder<DashboardError> dashboardErrors = new Page.Builder<DashboardError> ();
//		dashboardErrors.add(new DashboardError("id1", "datasetName1", "message1", LocalDateTime.now()));
//		dashboardErrors.add(new DashboardError("id2", "datasetName2", "message2", LocalDateTime.now()));
//		dashboardErrors.add(new DashboardError("id3", "Geluidszone bedrijventerrein ", "Fout tijdens bijwerken", LocalDateTime.now()));
		String datasetName = "Geluidszone bedrijventerrein";
		DashboardError error = new DashboardError("id1", DashboardErrorType.ERROR, new Message(DashboardErrorType.ERROR, null), datasetName, null, JobStateType.FAILED, JobType.HARVEST, LocalDateTime.now());
		dashboardErrors.add(error);
		datasetName = "Geluidszone vliegveld";
		error = new DashboardError("id2", DashboardErrorType.ERROR, new Message(DashboardErrorType.ERROR, null), datasetName, null, JobStateType.ABORTED, JobType.IMPORT, LocalDateTime.now());
		dashboardErrors.add(error);
		
		log.debug("sending DashboardError list");
		sender.tell (dashboardErrors.build (), self);
	}

	private void handleListDashboardActiveTasks(Object object) {
		log.debug ("handleDashboardActiveTaskList");
		
		final ActorRef sender = getSender(), self = getSelf();
		
		// TODO get content from joblog
		final Page.Builder<DashboardActiveTask> dashboardActiveTasks = new Page.Builder<DashboardActiveTask> ();
//		dashboardActiveTasks.add(new DashboardActiveTask("id1", "datasetName1", "message2", (int) Math.round(Math.random()*90.0) + 10));
//		dashboardActiveTasks.add(new DashboardActiveTask("id2", "Werkgelegenheid ", "Bezig met bijwerken",  (int) Math.round(Math.random()*90.0) + 10));
		String datasetName = "Werkgelegenheid";
		DashboardActiveTask activeTask = new DashboardActiveTask("id1", DashboardActiveTaskType.HARVESTER_STARTED, new Message(DashboardActiveTaskType.HARVESTER_STARTED, null), datasetName, (int)(Math.round(Math.random()*90.0) + 10), JobStateType.STARTED, JobType.HARVEST, LocalDateTime.now());
		dashboardActiveTasks.add(activeTask);
		datasetName = "Geluidszone vliegveld";
		activeTask = new DashboardActiveTask("id2", DashboardActiveTaskType.IMPORTER_STARTED, new Message(DashboardActiveTaskType.HARVESTER_STARTED, null), datasetName, (int)(Math.round(Math.random()*90.0) + 10), JobStateType.STARTED, JobType.IMPORT, LocalDateTime.now());
		dashboardActiveTasks.add(activeTask);
		
		log.debug("sending ActiveTask list");
		sender.tell (dashboardActiveTasks.build (), self);
	}

	private void handleListDashboardNotifications(Object object) {
		log.debug ("handleDashboardNotificationList");
		
		final ActorRef sender = getSender(), self = getSelf();
		
		// TODO get content from joblog / akka
		final Page.Builder<DashboardNotification> dashboardNotifications = new Page.Builder<DashboardNotification> ();
		String datasetName = "Sterrenwachten";
		DashboardNotification notifcation = new DashboardNotification("id1", DashboardNotificationType.STRUCTUURWIJZIGING, new Message(DashboardNotificationType.STRUCTUURWIJZIGING, null), datasetName, null, null, null, LocalDateTime.now());
		dashboardNotifications.add(notifcation);
		
		log.debug("sending DashboardNotification list");
		sender.tell (dashboardNotifications.build (), self);
	}

	private void handleEmptyList (final ListEntity<?> listEntity) {
		final Page.Builder<Category> builder = new Page.Builder<> ();
		
		sender ().tell (builder.build (), self ());
	}
	
	private void handleGetDataSource (final GetEntity<?> getEntity) {
		final DataSource dataSource = new DataSource (getEntity.id (), "DataSource: " + getEntity.id (), new Status (DataSourceStatusType.OK, LocalDateTime.now ()));
		
		sender ().tell (dataSource, self ());
	}
	
	private void handleGetCategory (final GetEntity<?> getEntity) {
		log.debug ("handleCategory");
		
		final ActorRef sender = getSender();
		
		final Future<Object> categoryInfo = Patterns.ask(database, new GetCategoryInfo(getEntity.id ()), 15000);
				categoryInfo.onSuccess(new OnSuccess<Object>() {
					@Override
					public void onSuccess(Object msg) throws Throwable {
						if(msg instanceof CategoryInfo) {
							CategoryInfo categoryInfo = (CategoryInfo)msg;
							log.debug("category info received");
							Category category = new Category (categoryInfo.getId(), categoryInfo.getName());
							log.debug("sending category: " + category);
							sender.tell (category, getSelf());
						} else {
							sender.tell (new NotFound(), getSelf());
						}
					}
				}, getContext().dispatcher());
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
							Dataset dataset = 
									new Dataset (datasetInfo.getId().toString(), datasetInfo.getName(),
											new Category(datasetInfo.getCategoryId(), datasetInfo.getCategoryName()),
											new Status (DataSourceStatusType.OK, LocalDateTime.now ()),
											null, // notification list
											new EntityRef (EntityType.SOURCE_DATASET, datasetInfo.getSourceDatasetId(), datasetInfo.getSourceDatasetName())
									);
							log.debug("sending dataset: " + dataset);
							sender.tell (dataset, getSelf());
						} else {
							sender.tell (new NotFound(), getSelf());
						}
					}
				}, getContext().dispatcher());
	}
	
	private void handleGetSourceDataset (final GetEntity<?> getEntity) {
		log.debug ("handleSourceDataset");
		
		final ActorRef sender = getSender();
		
		final Future<Object> sourceDatasetInfo = Patterns.ask(database, new GetSourceDatasetInfo(getEntity.id ()), 15000);
				sourceDatasetInfo.onSuccess(new OnSuccess<Object>() {
					@Override
					public void onSuccess(Object msg) throws Throwable {
						if(msg instanceof SourceDatasetInfo) {
							SourceDatasetInfo sourceDatasetInfo = (SourceDatasetInfo)msg;
							log.debug("sourcedataset info received");
							final SourceDataset sourceDataset = new SourceDataset (
									sourceDatasetInfo.getId(), 
									sourceDatasetInfo.getName(),
									new EntityRef (EntityType.CATEGORY, sourceDatasetInfo.getCategoryId(),sourceDatasetInfo.getCategoryName()),
									new EntityRef (EntityType.DATA_SOURCE, sourceDatasetInfo.getDataSourceId(), sourceDatasetInfo.getDataSourceName())
							);
							log.debug("sending source_dataset: " + sourceDataset);
							sender.tell (sourceDataset, getSelf());
						} else {
							sender.tell (new NotFound(), getSelf());
						}
					}
				}, getContext().dispatcher());
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

	private void handleListDatasets (final ListDatasets listDatasets) {
		String categoryId = listDatasets.categoryId();
		log.debug ("handleListDatasets categoryId=" + categoryId);
		
		final ActorRef sender = getSender(), self = getSelf();
		
		final Future<Object> datasetInfo = Patterns.ask(database, new GetDatasetListInfo(categoryId), 15000);
		
				datasetInfo.onSuccess(new OnSuccess<Object>() {

					@SuppressWarnings("unchecked")
					@Override
					public void onSuccess(Object msg) throws Throwable {
						List<DatasetInfo> datasetInfoList = (List<DatasetInfo>)msg;
						log.debug("data sources info received");
						
						final Page.Builder<Dataset> pageBuilder = new Page.Builder<> ();
						
						for(DatasetInfo datasetInfo : datasetInfoList) {
							final Dataset dataset =  new Dataset (datasetInfo.getId().toString(), datasetInfo.getName(),
									new Category(datasetInfo.getCategoryId(), datasetInfo.getCategoryName()),
									new Status (DataSourceStatusType.OK, LocalDateTime.now ()),
									null, // notification list
									new EntityRef (EntityType.SOURCE_DATASET, datasetInfo.getSourceDatasetId(), datasetInfo.getSourceDatasetName())
							);
							
							pageBuilder.add (dataset);
						}
						
						log.debug("sending dataset page");
						sender.tell (pageBuilder.build (), self);
					}
				}, getContext().dispatcher());
		
	}

}
