package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetActiveNotification.datasetActiveNotification;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QDatasetStatus.datasetStatus;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobLog.jobLog;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QNotification.notification;
import static nl.idgis.publisher.database.QNotificationResult.notificationResult;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLDeleteClause;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QSourceDatasetVersion;
import nl.idgis.publisher.database.messages.BaseDatasetInfo;
import nl.idgis.publisher.database.messages.DatasetInfo;
import nl.idgis.publisher.database.messages.GetDatasetInfo;
import nl.idgis.publisher.database.messages.GetNotifications;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.StoredNotification;
import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.job.NotificationProperties;
import nl.idgis.publisher.domain.job.harvest.HarvestNotificationType;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.query.GetDatasetByName;
import nl.idgis.publisher.domain.query.ListActiveNotifications;
import nl.idgis.publisher.domain.query.ListDatasets;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Category;
import nl.idgis.publisher.domain.web.DashboardItem;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.DatasetImportStatusType;
import nl.idgis.publisher.domain.web.DatasetStatusType;
import nl.idgis.publisher.domain.web.EntityRef;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Message;
import nl.idgis.publisher.domain.web.Notification;
import nl.idgis.publisher.domain.web.PutDataset;
import nl.idgis.publisher.domain.web.Status;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.TypedList;
import akka.actor.ActorRef;
import akka.actor.Props;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Order;
import com.mysema.query.types.path.PathBuilder;

public class DatasetAdmin extends AbstractAdmin {
	
	private final static PathBuilder<Long> layerCountPath = new PathBuilder<Long> (Long.class, "layerCount");
	private final static PathBuilder<Long> publishedServiceCountPath = new PathBuilder<Long> (Long.class, "publishedServiceCount");
	
	private final ObjectMapper objectMapper = new ObjectMapper ();

	public DatasetAdmin(ActorRef database) {
		super(database);
	}
	
	public static Props props(ActorRef database) {
		return Props.create(DatasetAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doQuery(ListDatasets.class, this::handleListDatasets);
		doQuery(ListActiveNotifications.class, this::handleListActiveNotifications);
		doList(Dataset.class, () -> handleListDatasets(null));
		doGet(Dataset.class, this::handleGetDataset);
		doDelete(Dataset.class, this::handleDeleteDataset);
		doPut(PutDataset.class, putDataset -> {
			try {
				if (putDataset.getOperation() == CrudOperation.CREATE){
					return handleCreateDataset(putDataset);
				} else{
					return handleUpdateDataset(putDataset);
				}
			} catch(JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		});
		doQueryOptional (GetDatasetByName.class, this::handleGetDatasetByName);
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
	
	private Notification createNotification (final StoredNotification storedNotification) {
		return new Notification (
				"" + storedNotification.getId (), 
				new Message (
					storedNotification.getType (), 
					new NotificationProperties (
							EntityType.DATASET, 
							storedNotification.getDataset ().getId (), 
							storedNotification.getDataset ().getName (),
							(ConfirmNotificationResult)storedNotification.getResult ()
						)
				)
			);
	}
	
	private Dataset createDataset (final DatasetInfo datasetInfo, final ObjectMapper objectMapper) throws Throwable {
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
				datasetInfo.getCategoryId () == null ? null : new Category(datasetInfo.getCategoryId(), datasetInfo.getCategoryName()),
				importStatus,
				notifications, // notification list
				new EntityRef (EntityType.SOURCE_DATASET, datasetInfo.getSourceDatasetId(), datasetInfo.getSourceDatasetName()),
				objectMapper.readValue (datasetInfo.getFilterConditions (), Filter.class),
				datasetInfo.getLayerCount (),
				datasetInfo.getPublishedServiceCount(),
				datasetInfo.isConfidential (),
				datasetInfo.isWmsOnly(),
				datasetInfo.getMetadataFileId ()
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
				notifications,
				t.get (layerCountPath),
				t.get (publishedServiceCountPath),
				t.get (sourceDatasetVersion.confidential),
				t.get (sourceDatasetVersion.wmsOnly),
				t.get (dataset.metadataFileIdentification)
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
	
	private CompletableFuture<Optional<Dataset>> handleGetDatasetByName (final GetDatasetByName getDataset) {
		if (getDataset.getName () == null || getDataset.getName ().isEmpty ()) {
			return CompletableFuture.completedFuture (Optional.<Dataset>empty ());
		}
		
		return db.transactional (tx -> {
			final AsyncSQLQuery query = datasetBaseQuery (tx);
			
			if (getDataset.isCaseSensitive ()) {
				query.where (dataset.name.eq (getDataset.getName ()));
			} else {
				query.where (dataset.name.equalsIgnoreCase (getDataset.getName ()));
			}
			
			return query
				.list (datasetPaths ())
				.thenApply (tuples -> {
					final List<Dataset> datasets = createDatasets (tuples); 

					if (datasets.isEmpty ()) {
						return Optional.empty ();
					} else {
						return Optional.of (datasets.get (0));
					}
				});
		});
	}
	
	private AsyncSQLQuery datasetBaseQuery (final AsyncHelper tx) {
		final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
		
		return tx.query().from(dataset)
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
		
	}
	
	private Object[] datasetPaths () {
		return new Object[] {
			dataset.identification,
			dataset.name,
			dataset.metadataFileIdentification,
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
			datasetActiveNotification.jobType,
			new SQLSubQuery ().from (leafLayer).where (leafLayer.datasetId.eq (dataset.id)).count ().as (layerCountPath),
			new SQLSubQuery ().from (publishedServiceDataset).where (publishedServiceDataset.datasetId.eq (dataset.id)).count ().as (publishedServiceCountPath),
			sourceDatasetVersion.confidential,
			sourceDatasetVersion.wmsOnly
		};
	}
	
	private List<DatasetInfo> createDatasetInfos (final TypedList<Tuple> tuples) {
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

		return Collections.unmodifiableList (datasetInfos);
	}
	
	private List<Dataset> createDatasets (final TypedList<Tuple> tuples) {
		final List<Dataset> datasets = new ArrayList<> ();
		
		for (final DatasetInfo datasetInfo: createDatasetInfos (tuples)) {
			try {
				datasets.add (createDataset (datasetInfo, objectMapper));
			} catch(Throwable t) {
				log.error("couldn't create dataset info: {}", t);
			}
		}
		
		return datasets;
	}
	
	private CompletableFuture<Page<Dataset>> handleListDatasets (final ListDatasets listDatasets) {
		log.debug ("handleListDatasets: {}", listDatasets);
		
		String categoryId = listDatasets.categoryId();
		DatasetStatusType status = listDatasets.status();
		long page = listDatasets.getPage();
		
		return db.transactional(tx -> {
			return tx.query().from(dataset)
				.count()
				.thenCompose(datasetCount -> {
					AsyncSQLQuery baseQuery = datasetBaseQuery (tx);
							
					if(categoryId != null) {
						baseQuery.where(category.identification.eq(categoryId));
					}
					
					if(status != null) {
						switch(status) {
							case IMPORTED:
								baseQuery.where(datasetStatus.imported.isTrue());
								break;
							case FAILURE:
								baseQuery.where(lastImportJob.finishState.eq(JobState.FAILED.name()));
								break;
							case WITH_MESSAGES:
								baseQuery.where(datasetActiveNotification.datasetId.isNotNull());
								break;
						}
					}
					
					if (listDatasets.getQuery () != null) {
						baseQuery.where (dataset.name.containsIgnoreCase (listDatasets.getQuery ().trim ()));
					}
					
					singlePage(baseQuery, page);
					
					final AsyncSQLQuery listQuery = baseQuery.clone ();
					
					return baseQuery
						.count ()
						.thenCompose ((count) -> {
							return listQuery
								.orderBy (dataset.name.asc ())
								.orderBy (datasetActiveNotification.jobCreateTime.desc ())
								.list (datasetPaths ())
								.thenApply(tuples -> {
										final Page.Builder<Dataset> pageBuilder = new Page.Builder<> ();
										
										for (final Dataset dataset: createDatasets (tuples)) {
											pageBuilder.add (dataset);
										}
										
										log.debug("sending dataset page");
										
										addPageInfo(pageBuilder, page, count);
										
										return pageBuilder.build ();
									});
							});
				});
		});
	}
	
	private CompletableFuture<Optional<Dataset>> handleGetDataset (String datasetId) {
		log.debug ("handleDataset");
		
		// TODO: stop using this database message and start using
		// the stuff in DatasetAdmin.handleListDatasets
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
	
	private CompletableFuture<Page<Notification>> handleListActiveNotifications (final ListActiveNotifications listNotifications) {
		final long page = listNotifications.getPage () != null ? Math.max (1, listNotifications.getPage ()) : 1;
		final long limit = listNotifications.getLimit () != null ? Math.max (1, listNotifications.getLimit ()) : DEFAULT_ITEMS_PER_PAGE;
		final long offset = Math.max (0, (page - 1) * limit);

		final CompletableFuture<List<Notification>> importNotificationsFuture = 
			f.ask(
				database, 
				new GetNotifications(
					Order.DESC,
					offset,
					limit,
					listNotifications.isIncludeRejected(),
					listNotifications.getSince()),
				InfoList.class)
					.thenApply(storedNotifications ->
						((InfoList<StoredNotification>)storedNotifications).getList().stream()
							.map(this::createNotification)
							.collect(Collectors.toList()));
		
		// TODO: fetch harvest notifications
		final CompletableFuture<List<Notification>> harvestNotificationsFuture = f.successful(Collections.emptyList());
		// TODO: query harvest notifications
		final CompletableFuture<List<Notification>> importNotificationsFuture = 
			f.ask(
				database, 
				new GetNotifications(
					Order.DESC,
					offset,
					limit,
					listNotifications.isIncludeRejected(),
					listNotifications.getSince()),
				InfoList.class)
					.thenApply(storedNotifications ->
						((InfoList<StoredNotification>)storedNotifications).getList().stream()
							.map(this::createNotification)
							.collect(Collectors.toList()));
		
		// TODO: fetch harvest notifications
		Notification demoNotification = new Notification(
			"id",
			new Message(
				HarvestNotificationType.NEW_SOURCE_DATASET,
				new NotificationProperties(
					EntityType.SOURCE_DATASET,
					"source_dataset_id",
					"source_dataset_name",
					null)));
		
		final CompletableFuture<List<Notification>> harvestNotificationsFuture = f.successful(Collections.singletonList(demoNotification));
		
		return 
			importNotificationsFuture.thenCompose(importNotifications -> 
			harvestNotificationsFuture.thenApply(harvestNotifications -> {
			
			final Page.Builder<Notification> dashboardNotifications = new Page.Builder<Notification>();
			
			dashboardNotifications.addAll(importNotifications);
			dashboardNotifications.addAll(harvestNotifications);
			
			// TODO: loop over harvest notifications
			
			// Paging:
			long count = importNotifications.size() + harvestNotifications.size();
			long pages = count / limit + Math.min(1, count % limit);
			
			if(pages > 1) {
				dashboardNotifications
					.setHasMorePages(true)
					.setPageCount(pages)
					.setCurrentPage(page);
			}
			
			return dashboardNotifications.build();
		}));
	}
	
	private CompletableFuture<Response<?>> deleteHelper (final String key, final AsyncSQLDeleteClause ... deleteClauses) {
		if (deleteClauses.length == 0) {
			throw new IllegalArgumentException ("At least one delete clause should be provided");
		}
		
		return deleteClauses[0]
			.execute ()
			.thenCompose (deleteCount -> {
				final List<AsyncSQLDeleteClause> tail = Arrays.asList (deleteClauses).subList (1, deleteClauses.length);
				
				return deleteHelper (key, deleteCount, tail);
			});
	}
	
	private CompletableFuture<Response<?>> deleteHelper (final String key, final long count, final List<AsyncSQLDeleteClause> deleteClauses) {
		if (deleteClauses.isEmpty ()) {
			return CompletableFuture.completedFuture (new Response<String>(CrudOperation.DELETE, count == 0 ? CrudResponse.NOK : CrudResponse.OK, key));
		}
		
		return deleteClauses
			.get (0)
			.execute ()
			.thenCompose (deleteCount -> deleteHelper (key, deleteCount, deleteClauses.subList (1, deleteClauses.size ())));
	}
	
	private CompletableFuture<Response<?>> handleDeleteDataset(String id) {
		return db
			.transactional (tx -> {
				
				final AsyncSQLDeleteClause deleteJobLog = tx
						.delete (jobLog)
						.where (new SQLSubQuery ()
							.from (importJob)
							.join (dataset).on (importJob.datasetId.eq (dataset.id))
							.join (jobState).on (jobState.jobId.eq (importJob.jobId))
							.where (jobLog.jobStateId.eq (jobState.id).and (dataset.identification.eq (id)))
							.exists ()
						);
				
				final AsyncSQLDeleteClause deleteJobState = tx
					.delete (jobState)
					.where (new SQLSubQuery ()
						.from (importJob)
						.join (dataset).on (importJob.datasetId.eq (dataset.id))
						.where (importJob.jobId.eq (jobState.jobId).and (dataset.identification.eq (id)))
						.exists ()
					);
				
				final AsyncSQLDeleteClause deleteNotificationResult = tx
					.delete (notificationResult)
					.where (new SQLSubQuery ()
						.from (importJob)
						.join (dataset).on (importJob.datasetId.eq (dataset.id))
						.join (notification).on (notification.jobId.eq (importJob.jobId))
						.where (notificationResult.notificationId.eq (notification.id).and (dataset.identification.eq (id)))
						.exists ()
					);
				
				final AsyncSQLDeleteClause deleteNotification = tx
					.delete (notification)
					.where (new SQLSubQuery ()
						.from (importJob)
						.join (dataset).on (importJob.datasetId.eq (dataset.id))
						.where (importJob.jobId.eq (notification.jobId).and (dataset.identification.eq (id)))
						.exists ()
					);
				
				final AsyncSQLDeleteClause deleteJob = tx
					.delete (job)
					.where (new SQLSubQuery ()
						.from (importJob)
						.join (dataset).on (importJob.datasetId.eq (dataset.id))
						.where (importJob.jobId.eq (job.id).and (dataset.identification.eq (id)))
						.exists ()
					);
				
				final AsyncSQLDeleteClause deleteDatasetColumn = tx
					.delete (datasetColumn)
					.where (new SQLSubQuery ()
						.from (dataset)
						.where (dataset.identification.eq (id).and (dataset.id.eq (datasetColumn.datasetId)))
						.exists()
					);

				final AsyncSQLDeleteClause deleteLayer = tx
					.delete (genericLayer)
					.where (new SQLSubQuery ()
						.from (leafLayer)
						.join (dataset).on (dataset.id.eq (leafLayer.datasetId))
						.where (
							leafLayer.genericLayerId.eq (genericLayer.id)
							.and (dataset.identification.eq (id))
						)
						.exists ()
					);
					
				final AsyncSQLDeleteClause deleteDataset = tx
					.delete (dataset)
					.where (dataset.identification.eq (id));
				
				return deleteHelper (id, deleteJobLog, deleteJobState, deleteNotificationResult, deleteNotification, deleteJob, deleteDatasetColumn, deleteLayer, deleteDataset);
			});
	}
	
	private CompletableFuture<Response<?>> handleCreateDataset(PutDataset putDataset) throws JsonProcessingException {
		log.debug ("handle create dataset: " + putDataset.id());
		
		String datasetIdent = UUID.randomUUID().toString();
		
		return db.transactional(tx ->
			tx.query().from(sourceDataset)
				.where(sourceDataset.identification.eq(putDataset.getSourceDatasetIdentification()))
				.singleResult(sourceDataset.id).thenCompose(sourceDatasetId -> {
					if(sourceDatasetId.isPresent()) {
						try {
							return tx.insert(dataset)
								.set(dataset.identification, datasetIdent)
								.set(dataset.metadataIdentification, UUID.randomUUID().toString())
								.set(dataset.metadataFileIdentification, UUID.randomUUID().toString())
								.set(dataset.name, putDataset.getDatasetName())
								.set(dataset.sourceDatasetId, sourceDatasetId.get())
								.set(dataset.filterConditions, 
									objectMapper.writeValueAsString(putDataset.getFilterConditions()))
								.executeWithKey(dataset.id).thenCompose(datasetId ->
									insertDatasetColumns(tx, datasetId.get(), putDataset.getColumnList()).thenApply(v ->
										new Response<String>(CrudOperation.CREATE, CrudResponse.OK, datasetIdent)));
						} catch(Exception e) {
							return f.failed(e);
						}
					} else {
						log.error("sourceDataset not found: " + putDataset.getSourceDatasetIdentification());
						return f.successful(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, datasetIdent));
					}
				}));
	}

	private CompletableFuture<List<Long>> insertDatasetColumns(AsyncHelper tx, Integer datasetId, List<Column> columns) {
		return f.sequence(
			StreamUtils.index(columns.stream())
				.map(indexedColumn -> {
					Column column = indexedColumn.getValue();
					
					return tx.insert(datasetColumn)
						.set(datasetColumn.datasetId, datasetId)
						.set(datasetColumn.index, indexedColumn.getIndex())
						.set(datasetColumn.name, column.getName())
						.set(datasetColumn.dataType, column.getDataType().toString())
						.execute();
				})
				.collect(Collectors.toList()));
	}

	private CompletableFuture<Response<?>> handleUpdateDataset(PutDataset putDataset) throws JsonProcessingException {
		String datasetIdent = putDataset.id();
		
		return db.transactional(tx ->
			tx.query().from(sourceDataset)
			.where(sourceDataset.identification.eq(putDataset.getSourceDatasetIdentification()))
			.singleResult(sourceDataset.id).thenCompose(sourceDatasetId -> {
				if(sourceDatasetId.isPresent()) {
					try {
						return tx.update(dataset)
							.set(dataset.name, putDataset.getDatasetName())
							.set(dataset.sourceDatasetId, sourceDatasetId.get())
							.set(dataset.filterConditions, 
								objectMapper.writeValueAsString(putDataset.getFilterConditions()))
							.where(dataset.identification.eq(datasetIdent))
							.execute().thenCompose(updateDataset ->
								tx.query().from(dataset)
								.where(dataset.identification.eq(datasetIdent))
								.singleResult(dataset.id).thenCompose(datasetId -> {
									if(datasetId.isPresent()) {									
										return tx.delete(datasetColumn)
										.where(datasetColumn.datasetId.eq(datasetId.get()))
										.execute().thenCompose(deleteDatasetColumn ->
											insertDatasetColumns(tx, datasetId.get(), putDataset.getColumnList()).thenApply(v ->
												new Response<String>(CrudOperation.CREATE, CrudResponse.OK, datasetIdent)));
									} else {
										return f.successful(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, datasetIdent));
									}
								}));
					} catch(Exception e) {
						return f.failed(e);
					}
				} else {
					log.error("sourceDataset not found: " + putDataset.getSourceDatasetIdentification());
					return f.successful(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, datasetIdent));
				}
			}));
	}
}
