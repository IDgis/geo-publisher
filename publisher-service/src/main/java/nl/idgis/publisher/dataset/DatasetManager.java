package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetCopy.datasetCopy;
import static nl.idgis.publisher.database.QDatasetView.datasetView;
import static nl.idgis.publisher.database.QHarvestNotification.harvestNotification;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QImportJobColumn.importJobColumn;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetMetadataAttachment.sourceDatasetMetadataAttachment;
import static nl.idgis.publisher.database.QSourceDatasetMetadataAttachmentError.sourceDatasetMetadataAttachmentError;
import static nl.idgis.publisher.database.QSourceDatasetUuidCount.sourceDatasetUuidCount;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QSourceDatasetVersionLog.sourceDatasetVersionLog;
import static nl.idgis.publisher.utils.StreamUtils.index;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.expr.DateTimeExpression;
import com.mysema.query.types.query.ListSubQuery;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;
import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.QSourceDatasetVersion;
import nl.idgis.publisher.database.messages.CopyTable;
import nl.idgis.publisher.database.messages.CreateView;
import nl.idgis.publisher.database.messages.DropTable;
import nl.idgis.publisher.database.messages.DropView;
import nl.idgis.publisher.database.messages.ReplaceTable;
import nl.idgis.publisher.database.projections.QColumn;
import nl.idgis.publisher.dataset.messages.AlreadyRegistered;
import nl.idgis.publisher.dataset.messages.Cleanup;
import nl.idgis.publisher.dataset.messages.DatasetCount;
import nl.idgis.publisher.dataset.messages.DeleteSourceDatasets;
import nl.idgis.publisher.dataset.messages.PrepareTable;
import nl.idgis.publisher.dataset.messages.PrepareView;
import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;
import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.DatasetLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.RasterDataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.UnavailableDataset;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.JsonUtils;

public class DatasetManager extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final AsyncHttpClient asyncHttpClient;

	private final ActorRef database;
	
	private final MetadataDocumentFactory mdf;

	private AsyncDatabaseHelper db;

	private FutureUtils f;

	public DatasetManager(ActorRef database) throws Exception {
		this.database = database;
		
		mdf = new MetadataDocumentFactory();
		asyncHttpClient = new AsyncHttpClient();
	}

	public static Props props(ActorRef database) {
		return Props.create(DatasetManager.class, database);
	}

	@Override
	public void preStart() throws Exception {
		log.debug("start");
		
		Timeout timeout = Timeout.apply(15000);

		f = new FutureUtils(getContext(), timeout);
		db = new AsyncDatabaseHelper(database, getClass().getName(), f, log);		
	}

	private <T> void returnToSender(CompletableFuture<T> future) {
		ActorRef sender = getSender();
		future.whenComplete((t, e) -> {
			if(e == null) {
				log.debug("answering: {}", t);
				
				sender.tell(t, getSelf());
			} else {
				Failure failure = new Failure(e);
				
				log.debug("answering with failure: {}", failure);
				
				sender.tell(failure, getSelf());
			}
		});
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof RegisterSourceDataset) {
			returnToSender(handleRegisterSourceDataset((RegisterSourceDataset) msg));
		} else if (msg instanceof Cleanup) {
			returnToSender (handleCleanup ((Cleanup) msg));
		} else if (msg instanceof DatasetCount) {
			returnToSender (handleDatasetCount((DatasetCount)msg));
		} else if (msg instanceof DeleteSourceDatasets) {
			returnToSender (handleDeleteSourceDatasets((DeleteSourceDatasets)msg));
		} else if (msg instanceof PrepareTable){
			returnToSender (handlePrepareTable((PrepareTable)msg));
		} else if (msg instanceof PrepareView) {
			returnToSender (handlePrepareView((PrepareView)msg));
		} else {
			unhandled(msg);
		}
	}
	
	private CompletableFuture<Object> handlePrepareView(PrepareView msg) {
		String datasetId = msg.getDatasetId();
		
		return db.transactional(msg, tx ->
			tx.delete(datasetCopy)
				.where(new SQLSubQuery().from(dataset)
					.where(dataset.id.eq(datasetCopy.datasetId))
					.where(dataset.identification.eq(datasetId))
					.exists())
				.execute().thenCompose(copyColumnCount ->
					tx.ask(new DropTable("data", datasetId)).thenCompose(dropTableResult ->
						dropTableResult instanceof Ack
							? tx.ask(new CreateView("data", datasetId, "staging_data", datasetId)).thenCompose(createViewResult ->
								createViewResult instanceof Ack
									? tx.insert(datasetView)
										.columns(
											datasetView.datasetId,
											datasetView.index,
											datasetView.name,
											datasetView.dataType)
										.select(subselectDatasetColumns(datasetId))
										.execute().thenApply(viewColumnCount -> (Object)new Ack())
									: f.successful(createViewResult))
							: f.successful(dropTableResult))));
	}

	private ListSubQuery<Tuple> subselectDatasetColumns(String datasetId) {
		return new SQLSubQuery().from(importJobColumn)
			.join(importJob).on(importJob.id.eq(importJobColumn.importJobId))
			.where(importJob.id.in(
				new SQLSubQuery().from(importJob)
					.join(jobState).on(jobState.jobId.eq(importJob.jobId))
					.join(dataset).on(dataset.id.eq(importJob.datasetId))
					.where(jobState.state.eq(JobState.SUCCEEDED.name()))
					.where(dataset.identification.eq(datasetId))
					.orderBy(jobState.createTime.desc())
					.limit(1)
					.list(importJob.id)))
			.list(
				importJob.datasetId,
				importJobColumn.index,
				importJobColumn.name,
				importJobColumn.dataType);
	}
	
	private CompletableFuture<Object> makeDatasetCopy(AsyncHelper tx, String tmpTable, String datasetId, long insertCount) {
		log.debug("making dataset copy");
		
		long timeout = insertCount * 3 + 15000;
		log.debug("Creating dataset copy on dataset {} with a timeout of: {} ms", datasetId, timeout);
		
		return tx.ask(new DropView("data", datasetId)).thenCompose(dropViewResult ->
			dropViewResult instanceof Ack
				? tx.delete(datasetView)
					.where(new SQLSubQuery().from(dataset)
						.where(dataset.identification.eq(datasetId))
						.where(dataset.id.eq(datasetView.datasetId))
						.exists())
					.execute()
						.thenCompose(cnt ->
							tx.ask(new CopyTable("data", datasetId, "staging_data", datasetId), timeout)).thenCompose(copyTableResult ->
								tx.insert(datasetCopy)
									.columns(
										datasetCopy.datasetId,
										datasetCopy.index,
										datasetCopy.name,
										datasetCopy.dataType)
									.select(subselectDatasetColumns(datasetId))
									.execute().thenCompose(cnt -> 
										tx.ask(new ReplaceTable("staging_data", tmpTable, datasetId))))
				: f.successful(dropViewResult));
	}
	
	private CompletableFuture<Object> keepDatasetView(AsyncHelper tx, String tmpTable, String datasetId) {
		log.debug("keeping dataset view");
		
		return tx.ask(new DropView("data", datasetId)).thenCompose(dropViewResult ->
			dropViewResult instanceof Ack
				? tx.ask(new ReplaceTable("staging_data", tmpTable, datasetId)).thenCompose(createTableResult ->
					createTableResult instanceof Ack
						? tx.ask(new CreateView("data", datasetId, "staging_data", datasetId))
						: f.successful(createTableResult))
				: f.successful(dropViewResult));
	}

	private CompletableFuture<Object> handlePrepareTable(PrepareTable msg) {
		log.debug("preparing table: {}", msg);
		
		String tmpTable = msg.getTmpTable();
		String datasetId = msg.getDatasetId();
		List<Column> columns = msg.getColumns();
		long insertCount = msg.getInsertCount();
		
		return db.transactional(msg, tx ->
			fetchViewInfo(tx, datasetId).thenCompose(viewColumns ->
				viewColumns.isEmpty() // view present?
					? tx.ask(new ReplaceTable("staging_data", tmpTable, datasetId)) 
					: viewColumns.equals(columns) // same columns?
						? keepDatasetView(tx, tmpTable, datasetId)
						: makeDatasetCopy(tx, tmpTable, datasetId, insertCount)));
	}

	private CompletableFuture<List<Column>> fetchViewInfo(AsyncHelper tx, String datasetId) {
		log.debug("fetching dataset view info");
		
		return tx.query().from(datasetView)
			.join(dataset).on(dataset.id.eq(datasetView.datasetId))
			.where(dataset.identification.eq(datasetId))
			.orderBy(datasetView.index.asc())
			.list(
				datasetView.name, 
				datasetView.dataType).thenApply(viewInfo ->
					viewInfo.list().stream()
						.map(t -> new Column(
							t.get(datasetView.name), 
							t.get(datasetView.dataType),
							null /* column alias (irrelevant in this context) */))
						.collect(Collectors.toList()));
	}

	private CompletableFuture<Long> handleDeleteSourceDatasets(DeleteSourceDatasets msg) {
		return
			db.update(sourceDataset)
			.set(sourceDataset.deleteTime, DateTimeExpression.currentTimestamp(Timestamp.class))
			.where(sourceDataset.externalIdentification.in(msg.getDatasetIds())
					.and(sourceDataset.dataSourceId.eq(
							new SQLSubQuery()
								.from(dataSource)
								.where(dataSource.identification.eq(msg.getDataSourceId()))
								.unique(dataSource.id)
							)))
			.execute().thenCompose(count -> {
				return 
					db.insert(harvestNotification)
					.columns(harvestNotification.notificationType,
						harvestNotification.sourceDatasetId,
						harvestNotification.sourceDatasetVersionId,
						harvestNotification.done)
					.select(
							new SQLSubQuery()
							.from(sourceDatasetVersion)
							.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
							.where(sourceDataset.externalIdentification.in(msg.getDatasetIds())
									.and(sourceDatasetVersion.id.in(
											new SQLSubQuery().from(sourceDatasetVersion)
												.join(sourceDataset).on(sourceDataset.id
														.eq(sourceDatasetVersion.sourceDatasetId))
												.where(sourceDataset.externalIdentification.in(msg.getDatasetIds()))
												.list(sourceDatasetVersion.id.max()))))
					.list("SOURCE_DATASET_DELETED",
							sourceDataset.id,
							sourceDatasetVersion.id,
							false))
					.execute();
			});
	}
	
	private CompletableFuture<Void> handleDatasetCount(DatasetCount msg) {
		
		db.query().from(sourceDatasetUuidCount)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetUuidCount.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.where(dataSource.identification.eq(msg.getDatasourceId())
				.and(sourceDataset.externalIdentification.notIn(msg.getDatasetCount().keySet())))
			.list(sourceDatasetUuidCount.sourceDatasetId).thenCompose(ids -> {
				ids.list().forEach(id -> {
					db.update(sourceDatasetUuidCount)
						.set(sourceDatasetUuidCount.count, 0)
						.where(sourceDatasetUuidCount.sourceDatasetId.eq(id))
						.execute();
				});
				
				return f.successful(null);
			});
		
		for(Map.Entry<String, Integer> entry : msg.getDatasetCount().entrySet()) {
			String externalIdentification = entry.getKey();
			Integer count = entry.getValue();
			
			db.query().from(sourceDataset)
				.where(sourceDataset.externalIdentification.eq(externalIdentification))
				.singleResult(sourceDataset.id).thenCompose(sourceDatasetId -> {
					sourceDatasetId.ifPresent(id -> {
						db.query().from(sourceDatasetUuidCount)
								.where(sourceDatasetUuidCount.sourceDatasetId.eq(id))
								.exists().thenCompose(exists -> {
							if(exists) {
								db.update(sourceDatasetUuidCount)
									.set(sourceDatasetUuidCount.count, count)
									.where(sourceDatasetUuidCount.sourceDatasetId.eq(id))
									.execute();
							} else {
								db.insert(sourceDatasetUuidCount)
									.set(sourceDatasetUuidCount.sourceDatasetId, id)
									.set(sourceDatasetUuidCount.count, count)
									.execute();
							}
							
							return f.successful(null);
						});
					});
					
					return f.successful(null);
				});
		}
		
		return f.successful(null);
	}

	private CompletableFuture<Optional<Integer>> getCategoryId(final AsyncHelper tx, final String identification) {
		log.debug("get category id: {}", identification);
		
		// category can be null for unavailable datasets
		if(identification == null) {
			return f.successful(Optional.empty());
		}
		
		return
			tx.query().from(category)
				.where(category.identification.eq(identification))
				.singleResult(category.id).thenCompose(id -> {
					if(id.isPresent()) {
						return f.successful(id);
					} else {
						return tx.insert(category)
							.set(category.identification, identification)
							.set(category.name, identification)
							.executeWithKey(category.id);
					}
				});
	}
	
	private CompletableFuture<Dataset> getSourceDatasetVersion(final AsyncHelper tx, final Integer versionId) {
		log.debug("retrieving source dataset version: {}", versionId);
		
		return
			f.collect(
				tx.query().from(sourceDatasetVersion)
					.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
					.leftJoin(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
					.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))			
					.leftJoin(category).on(category.id.eq(sourceDatasetVersion.categoryId))
					.where(sourceDatasetVersion.id.eq(versionId))
					.singleResult(
						sourceDataset.externalIdentification,
						sourceDatasetVersion.name,
						sourceDatasetVersion.alternateTitle,
						sourceDatasetVersion.type,
						category.identification,
						sourceDatasetVersion.revision,
						sourceDatasetVersion.confidential,
						sourceDatasetVersion.wmsOnly,
						sourceDatasetVersion.metadataConfidential,
						sourceDatasetVersion.physicalName,
						sourceDatasetVersion.refreshFrequency,
						sourceDatasetVersion.archived))
			.collect(
				tx.query().from(sourceDatasetVersionLog)
				.where(sourceDatasetVersionLog.sourceDatasetVersionId.eq(versionId))
				.list(
					sourceDatasetVersionLog.level,
					sourceDatasetVersionLog.type,
					sourceDatasetVersionLog.content))
			.collect(			
				tx.query().from(sourceDatasetVersionColumn)
				.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(versionId))
				.orderBy(sourceDatasetVersionColumn.index.asc())
				.list(new QColumn(
					sourceDatasetVersionColumn.name,
					sourceDatasetVersionColumn.dataType,
					sourceDatasetVersionColumn.alias))).thenApply((baseInfoOptional, logInfo, columnInfo) -> {
						Tuple baseInfo = baseInfoOptional.orElseThrow(() -> new IllegalArgumentException("source dataset version missing"));
						
						String id = baseInfo.get(sourceDataset.externalIdentification);
						String name = baseInfo.get(sourceDatasetVersion.name);
						String alternateTitle = baseInfo.get(sourceDatasetVersion.alternateTitle);
						String type = baseInfo.get(sourceDatasetVersion.type);
						String categoryId = baseInfo.get(category.identification);
						Timestamp revision = baseInfo.get(sourceDatasetVersion.revision);
						boolean confidential = baseInfo.get(sourceDatasetVersion.confidential);
						boolean metadataConfidential = baseInfo.get(sourceDatasetVersion.metadataConfidential);
						boolean wmsOnly = baseInfo.get(sourceDatasetVersion.wmsOnly);
						byte[] metadataContent = baseInfo.get(sourceDatasetMetadata.document);
						String physicalName = baseInfo.get(sourceDatasetVersion.physicalName);
						String refreshFrequency = baseInfo.get(sourceDatasetVersion.refreshFrequency);
						boolean archived = baseInfo.get(sourceDatasetVersion.archived);
						
						MetadataDocument metadata;
						if(metadataContent == null) {
							metadata = null;
						} else {
							try {
								metadata = mdf.parseDocument(metadataContent);
							} catch(Exception e) {
								metadata = null;
							}
						}
						
						ZonedDateTime revisionDate;
						
						// convert from timestamp to zonedDateTime because harvester provides
						// zonedDateTime objects, otherwise source dataset versions are never equal.
						if(revision != null) {
							revisionDate = 
									revision
										.toLocalDateTime()
										.toLocalDate()
										.atStartOfDay(ZoneId.of("Europe/Amsterdam"));
						} else {
							revisionDate = null;
						}
						
						Set<Log> logs = new HashSet<>();
						for(Tuple logTuple : logInfo) {
							log.debug("retrieving log");
							
							try {
								LogLevel logLevel = LogLevel.valueOf(logTuple.get(sourceDatasetVersionLog.level));
								DatasetLogType logType = DatasetLogType.valueOf(logTuple.get(sourceDatasetVersionLog.type));
								
								final DatasetLog<?> logContent;
								Class<? extends DatasetLog<?>> logContentClass = logType.getContentClass();
								if(logContentClass != null) {
									logContent = JsonUtils.fromJson(logContentClass, logTuple.get(sourceDatasetVersionLog.content));
								} else {
									logContent = null;
								}
								
								Log logLine = Log.create(logLevel, logType, logContent);
								log.debug("logLine: {}", logLine);
								
								logs.add(logLine);
							} catch(Exception e) {
								log.error("processing log failed: {}", e);
							}
						}
						
						log.debug("constructing dataset");
						
						final Dataset dataset;
						switch(type) {
							case "VECTOR":
								dataset = new VectorDataset(
									id, 
									name,
									alternateTitle,
									categoryId, 
									revisionDate, 
									logs, 
									confidential,
									metadataConfidential,
									wmsOnly,
									metadata,
									new Table(columnInfo.list()),
									physicalName,
									refreshFrequency,
									archived);
								break;
							case "RASTER":
								dataset = new RasterDataset(
									id,
									name,
									alternateTitle,
									categoryId,
									revisionDate,
									logs,
									confidential,
									metadataConfidential,
									wmsOnly,
									metadata,
									physicalName,
									refreshFrequency,
									archived);
								break;
							default:
								dataset = new UnavailableDataset(
									id,
									name,
									alternateTitle,
									categoryId,
									revisionDate,
									logs,
									confidential,
									metadataConfidential,
									wmsOnly,
									metadata,
									physicalName,
									refreshFrequency,
									archived);
								break;
						}
						
						log.debug("current source dataset version: {}", dataset);
						
						return dataset;
			});
	}
	
	private CompletableFuture<Optional<Dataset>> getCurrentSourceDatasetVersion(final AsyncHelper tx, final String dataSourceIdentification, final String identification) {
		log.debug("get current source dataset version");
		
		return
			tx.query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
						.and(sourceDataset.externalIdentification.eq(identification)))
				.singleResult(sourceDatasetVersion.id.max()).thenCompose(maxVersionId -> {
					if(maxVersionId.isPresent()) {
						return getSourceDatasetVersion(tx, maxVersionId.get()).thenApply(Optional::of);
					} else {
						return f.<Optional<Dataset>>successful(Optional.empty());
					}
					
				}).thenCompose(optionalDataset -> {
					if(optionalDataset.isPresent()) {
						return ensureNotDeleted(tx, identification)
							.thenApply(ensured -> optionalDataset);
					} else {
						return f.successful(optionalDataset);
					}
				});
	}
	
	private CompletableFuture<Object> insertSourceDatasetVersion(AsyncHelper tx, String dataSourceIdentification, Dataset dataset) {
		log.debug("inserting source dataset (by dataSource identification)");
		
		return 
			tx.query().from(sourceDataset)
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
					.and(sourceDataset.externalIdentification.eq(dataset.getId())))
				.singleResult(sourceDataset.id).thenCompose(sourceDatasetIdOptional -> {
					Integer sourceDatasetId = sourceDatasetIdOptional.orElseThrow(() -> new IllegalStateException("source dataset id missing"));
					
					return getCurrentSourceDatasetVersion(tx, dataSourceIdentification, dataset.getId())
						.thenCompose(optionalDataset -> {
							if(optionalDataset.isPresent()) {
								return insertSourceDatasetVersion(tx, sourceDatasetId, dataset, optionalDataset).thenApply(v -> new Updated());
							} else {
								return insertSourceDatasetVersion(tx, sourceDatasetId, dataset, Optional.empty()).thenApply(v -> new Updated());
							}
						});
				});
	}
	
	private CompletableFuture<Void> insertSourceDatasetVersion(AsyncHelper tx, Integer sourceDatasetId, Dataset dataset, Optional<Dataset> previousDatasetOptional) {
		log.debug("inserting source dataset (by id)");
		
		final String type;
		if(dataset instanceof VectorDataset) {
			type = "VECTOR";
		} else if(dataset instanceof RasterDataset) {
			type = "RASTER";
		} else if(dataset instanceof UnavailableDataset) {
			type = "UNAVAILABLE";
		} else {
			type = "UNKNOWN";
		}
		
		log.debug("type: {}", type);
		
		return getCategoryId(tx, dataset.getCategoryId()).thenCompose(categoryId -> {
			log.debug("categoryId: {}", categoryId);
			
			final Timestamp revision;
			ZonedDateTime revisionDate = dataset.getRevisionDate();
			if(revisionDate != null) {
				revision = Timestamp.valueOf(revisionDate.toLocalDateTime());
			} else {
				revision = null;
			}
			
			String name = dataset.getName();
			String alternateTitle = dataset.getAlternateTitle();
			boolean confidential = dataset.isConfidential();
			boolean metadataConfidential = dataset.isMetadataConfidential();
			boolean wmsOnly = dataset.isWmsOnly();
			String physicalName = dataset.getPhysicalName();
			String refreshFrequency = dataset.getRefreshFrequency();
			boolean archived = dataset.isArchived();
			
			return tx.insert(sourceDatasetVersion)
				.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
				.set(sourceDatasetVersion.type, type)
				.set(sourceDatasetVersion.name, name)		
				.set(sourceDatasetVersion.alternateTitle, alternateTitle)
				.set(sourceDatasetVersion.categoryId, categoryId.orElse(null))
				.set(sourceDatasetVersion.revision, revision)
				.set(sourceDatasetVersion.confidential, confidential)
				.set(sourceDatasetVersion.metadataConfidential, metadataConfidential)
				.set(sourceDatasetVersion.wmsOnly, wmsOnly)
				.set(sourceDatasetVersion.physicalName, physicalName)
				.set(sourceDatasetVersion.refreshFrequency, refreshFrequency)
				.set(sourceDatasetVersion.archived, archived)
				.executeWithKey(sourceDatasetVersion.id);
			}).thenCompose(sourceDatasetVersionId -> {
				log.debug("sourceDatasetVersionId: {}", sourceDatasetVersionId);
				
				if(!previousDatasetOptional.isPresent()) {
					tx.insert(harvestNotification)
						.set(harvestNotification.notificationType, "NEW_SOURCE_DATASET")
						.set(harvestNotification.sourceDatasetId, sourceDatasetId)
						.set(harvestNotification.sourceDatasetVersionId, sourceDatasetVersionId.get())
						.set(harvestNotification.done, false)
						.execute();
				} else {
					Dataset previousDataset = previousDatasetOptional.get();
					
					if(previousDataset.isConfidential() != dataset.isConfidential()) {
						tx.insert(harvestNotification)
							.set(harvestNotification.notificationType, "CONFIDENTIAL_CHANGED")
							.set(harvestNotification.sourceDatasetId, sourceDatasetId)
							.set(harvestNotification.sourceDatasetVersionId, sourceDatasetVersionId.get())
							.set(harvestNotification.done, false)
							.execute();
					}
					
					if(previousDataset.isWmsOnly() != dataset.isWmsOnly()) {
						tx.insert(harvestNotification)
							.set(harvestNotification.notificationType, "WMS_ONLY_CHANGED")
							.set(harvestNotification.sourceDatasetId, sourceDatasetId)
							.set(harvestNotification.sourceDatasetVersionId, sourceDatasetVersionId.get())
							.set(harvestNotification.done, false)
							.execute();
					}
					
					final String previousDatasetType;
					if(previousDataset instanceof VectorDataset) {
						previousDatasetType = "VECTOR";
					} else if(previousDataset instanceof RasterDataset) {
						previousDatasetType = "RASTER";
					} else if(previousDataset instanceof UnavailableDataset) {
						previousDatasetType = "UNAVAILABLE";
					} else {
						previousDatasetType = "UNKNOWN";
					}
					
					if(!"UNAVAILABLE".equals(previousDatasetType) && "UNAVAILABLE".equals(type)) {
						tx.insert(harvestNotification)
							.set(harvestNotification.notificationType, "SOURCE_DATASET_UNAVAILABLE")
							.set(harvestNotification.sourceDatasetId, sourceDatasetId)
							.set(harvestNotification.sourceDatasetVersionId, sourceDatasetVersionId.get())
							.set(harvestNotification.done, false)
							.execute();
					}
				}
				
				return insertSourceDatasetVersionLogs(tx, sourceDatasetVersionId.get(), dataset).thenCompose(v -> {					
					if(dataset instanceof VectorDataset) {
						return insertSourceDatasetVersionColumns(tx, sourceDatasetVersionId.get(), (VectorDataset)dataset);
					} else {
						return f.successful(null);
					}
				});
			});
	}
	
	private CompletionStage<Void> insertSourceDatasetVersionLogs(AsyncHelper tx, Integer sourceDatasetVersionId, Dataset dataset) {
		log.debug("inserting logs");
		
		return dataset.getLogs().stream()
			.map(logLine -> {
				log.debug("logLine: {}", logLine);
				
				String contentValue = null;
				
				MessageProperties content = logLine.getContent();
				if(content != null) {
					try {
						contentValue = JsonUtils.toJson(content);
					} catch(Exception e) {
						log.error("storing log content failed: {}", e);
					}
				}
				
				return tx.insert(sourceDatasetVersionLog)
					.set(sourceDatasetVersionLog.sourceDatasetVersionId, sourceDatasetVersionId)
					.set(sourceDatasetVersionLog.level, logLine.getLevel().name())
					.set(sourceDatasetVersionLog.type, logLine.getType().name())
					.set(sourceDatasetVersionLog.content, contentValue)
					.execute();
			})
			.reduce(f.successful(0l), DatasetManager::sum)
			.thenApply(l -> {
				log.debug("number of logs stored: {}", l);
				
				return null;
			});
	}
	
	private static CompletableFuture<Long> sum(CompletableFuture<Long> a, CompletableFuture<Long> b) {
		return a.thenCompose(c -> b.thenApply(d -> c + d));
	}

	private CompletionStage<Void> insertSourceDatasetVersionColumns(AsyncHelper tx, Integer sourceDatasetVersionId, VectorDataset vectorDataset) {
		log.debug("inserting columns");
		
		Table table = vectorDataset.getTable();
		
		return index(table.getColumns().stream())
			.map(indexedColumn -> {
				Column column = indexedColumn.getValue();
				
				return tx.insert(sourceDatasetVersionColumn)
					.set(sourceDatasetVersionColumn.sourceDatasetVersionId, sourceDatasetVersionId)
					.set(sourceDatasetVersionColumn.index, indexedColumn.getIndex())
					.set(sourceDatasetVersionColumn.name, column.getName())
					.set(sourceDatasetVersionColumn.dataType, column.getDataType().toString())
					.set(sourceDatasetVersionColumn.alias, column.getAlias())
					.execute();
			})			
			.reduce(f.successful(0l), DatasetManager::sum)
			.thenApply(l -> {
				log.debug("number of columns stored: {}", l);
				
				return null;
			});
	}
	
	private CompletableFuture<Object> insertSourceDataset(AsyncHelper tx, String dataSourceIdentification, Dataset dataset) {
		log.debug("inserting source dataset");
		
		return
			tx.insert(sourceDataset)
				.columns(
					sourceDataset.dataSourceId,
					sourceDataset.externalIdentification,
					sourceDataset.identification,
					sourceDataset.metadataIdentification,
					sourceDataset.metadataFileIdentification)
				.select(new SQLSubQuery()
					.from(dataSource)
					.where(dataSource.identification.eq(dataSourceIdentification))
					.list(
						dataSource.id, 
						dataset.getId(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString(),
						UUID.randomUUID().toString()))
				.executeWithKey(sourceDataset.id).thenCompose(sourceDatasetId -> 
					insertSourceDatasetVersion(tx, sourceDatasetId.get(), dataset, Optional.empty())).thenApply(v -> new Registered());
	}
	
	private CompletableFuture<Long> ensureNotDeleted(AsyncHelper tx, String datasetId) {
		return 
			tx.update(sourceDataset)
			.setNull(sourceDataset.deleteTime)
			.where(sourceDataset.externalIdentification.eq(datasetId))
			.execute();
	}
	
	private CompletableFuture<Void> downloadMetadataAttachment(AsyncHelper tx, int sourceDatasetId, String identification, URL url) {
		log.debug("downloading metadata attachment: " + url);
		
		CompletableFuture<Void> future = new CompletableFuture<>();
		
		asyncHttpClient.prepareGet(url.toExternalForm())
			.setFollowRedirects(true)
			.execute(new AsyncCompletionHandler<Response>() {
				
				@Override
				public Response onCompleted(Response response) throws Exception {
					int statusCode = response.getStatusCode();
					if(statusCode >= 200 && statusCode < 400) {
						log.debug("metadata attachment download completed");
						
						String contentType = response.getContentType();
						String contentDisposition = response.getHeader("Content-Disposition");
						byte[] content = response.getResponseBodyAsBytes();
						
						tx.insert(sourceDatasetMetadataAttachment)
							.set(sourceDatasetMetadataAttachment.identification, identification)
							.set(sourceDatasetMetadataAttachment.sourceDatasetId, sourceDatasetId)
							.set(sourceDatasetMetadataAttachment.contentType, contentType)
							.set(sourceDatasetMetadataAttachment.contentDisposition, contentDisposition)
							.set(sourceDatasetMetadataAttachment.content, content)
							.execute().thenRun(() -> {
								log.debug("metadata attachment stored");
								future.complete(null); 
							});
					} else {
						log.warning("unexpected http status code: " + statusCode);
						
						tx.insert(sourceDatasetMetadataAttachmentError)
							.set(sourceDatasetMetadataAttachmentError.identification, identification)
							.set(sourceDatasetMetadataAttachmentError.sourceDatasetId, sourceDatasetId)
							.set(sourceDatasetMetadataAttachmentError.httpStatus, statusCode)
							.execute().thenRun(() -> {
								log.debug("metadata attachment error stored");
								future.complete(null); 
							});
					}
					
					return response;
				}
				
				@Override
				public void onThrowable(Throwable t) {
					log.warning("metadata attachment download failed");

					tx.insert(sourceDatasetMetadataAttachmentError)
						.set(sourceDatasetMetadataAttachmentError.identification, identification)
						.set(sourceDatasetMetadataAttachmentError.sourceDatasetId, sourceDatasetId)
						.execute().thenRun(() -> {
							log.debug("metadata attachment error stored");
							future.complete(null); 
						});
				}
				
			});
		
		return future;
	}
	
	private CompletableFuture<Void> updateMetadataAttachments(AsyncHelper tx, boolean removeExisting, int sourceDatasetId, MetadataDocument metadata) {
		log.debug("updating metadata attachments");
		
		final CompletableFuture<Void> deletePreviousErrors =
			tx.delete(sourceDatasetMetadataAttachmentError)
				.where(sourceDatasetMetadataAttachmentError.sourceDatasetId.eq(sourceDatasetId))
				.execute().thenApply(result -> null);
		
		final CompletableFuture<Set<String>> determineExistingIds;
		if(removeExisting) {
			determineExistingIds = tx.delete(sourceDatasetMetadataAttachment)
			.where(sourceDatasetMetadataAttachment.sourceDatasetId.eq(sourceDatasetId))
			.execute().thenApply(deleteResult -> { 
				log.debug("existing metadata attachments deleted");
				return Collections.emptySet();
			});
		} else {
			determineExistingIds = tx.query().from(sourceDatasetMetadataAttachment)
				.where(sourceDatasetMetadataAttachment.sourceDatasetId.eq(sourceDatasetId))
				.list(sourceDatasetMetadataAttachment.identification).thenApply(typedList -> {
					log.debug("existing metadata attachment ids retrieved");
					return typedList.list().stream().collect(Collectors.toSet()); 
				});
		}
		
		return deletePreviousErrors.thenCompose(previousErrorsDeleted ->
		
		determineExistingIds.thenCompose(existingIds -> {
				ArrayList<CompletableFuture<Void>> pendingDownloads = new ArrayList<>();
				for(String supplementalInformation : metadata.getSupplementalInformation()) {
					log.debug("supplemental information: " + supplementalInformation);
					
					if(existingIds.contains(supplementalInformation)) {
						log.debug("attachment already downloaded -> skip");
						continue;
					}
					
					int separator = supplementalInformation.indexOf("|");
					if(separator != -1) {
						String urlPart = supplementalInformation.substring(separator + 1).trim().replace("\\", "/");
						try {
							pendingDownloads.add(downloadMetadataAttachment(tx, sourceDatasetId, supplementalInformation, new URL(urlPart)));
						} catch(MalformedURLException e) {
							log.warning("not a valid url: " + urlPart, e);
						}
					}
				}
				
				for(String browseGraphic : metadata.getDatasetBrowseGraphics()) {
					log.debug("browse graphic: " + browseGraphic);
					
					if(existingIds.contains(browseGraphic)) {
						log.debug("attachment already downloaded -> skip");
						continue;
					}
					
					try {
						String url = browseGraphic.trim().replace("\\", "/");
						pendingDownloads.add(downloadMetadataAttachment(tx, sourceDatasetId, browseGraphic, new URL(url)));
					} catch(MalformedURLException e) {
						log.warning("not a valid url: " + browseGraphic, e);
					}
				}
				
				return f.sequence(pendingDownloads).thenApply(downloadResults -> {
					log.debug("all metadata attachment downloads completed");
					return null;
				});
		}));
	}
	
	private CompletableFuture<Void> updateMetadata(AsyncHelper tx, String dataSourceIdentification, String identification, Optional<MetadataDocument> metadata) {
		if(metadata.isPresent()) {
			log.debug("updating metadata");
			
			try {
				byte[] newDocument = metadata.get().getContent();
				
				return tx.query().from(sourceDataset)
					.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
					.leftJoin(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
					.where(dataSource.identification.eq(dataSourceIdentification))
					.where(sourceDataset.externalIdentification.eq(identification))
					.singleResult(sourceDataset.id, sourceDatasetMetadata.document).thenCompose(optionalCurrentInfo -> {
						if(optionalCurrentInfo.isPresent()) {
							Tuple currentInfo = optionalCurrentInfo.get();
							int id = currentInfo.get(sourceDataset.id);
							byte[] currentDocument = currentInfo.get(sourceDatasetMetadata.document);
							
							if(currentDocument == null) {
								log.debug("no metadata document found -> insert");
								
								return tx.insert(sourceDatasetMetadata)
								.set(sourceDatasetMetadata.document, newDocument)
								.set(sourceDatasetMetadata.sourceDatasetId, id)
								.execute().thenCompose(insertResult -> updateMetadataAttachments(tx, true, id, metadata.get()));
							} else {
								log.debug("metadata document found");
								try {
									MessageDigest md = MessageDigest.getInstance("MD5");
									if(Arrays.equals(md.digest(newDocument), md.digest(currentDocument))) {
										log.debug("same hash -> only download missing attachments");
										return updateMetadataAttachments(tx, false, id, metadata.get());
									}
									
									log.debug("different hash -> update");
									return tx.update(sourceDatasetMetadata)
									.set(sourceDatasetMetadata.document, newDocument)
									.where(sourceDatasetMetadata.sourceDatasetId.eq(id))
									.execute().thenCompose(updateResult -> updateMetadataAttachments(tx, true, id, metadata.get()));
								} catch(Exception e) {
									return f.failed(e);
								}
							}
						}
							
						return f.failed(new IllegalStateException("source dataset not found"));
					});
			} catch(Exception e) {
				log.error("failed to store metadata for source dataset: {}, exception: {}", e);
				return f.successful(null);
			}
		} else {
			log.debug("dataset doesn't have metadata");
			return f.successful(null);
		}
	}

	private CompletableFuture<Object> handleRegisterSourceDataset(final RegisterSourceDataset msg) {
		log.debug("registering source dataset");
		
		Dataset dataset = msg.getDataset();
		String dataSourceIdentification = msg.getDataSource();
		
		return db.transactional(tx -> {
			String identification = dataset.getId();
			
			CompletableFuture<Object> registerResult = getCurrentSourceDatasetVersion(tx, dataSourceIdentification, identification).thenCompose(currentVersion -> 
				currentVersion.isPresent()
					? currentVersion.get().equals(dataset)
						? f.successful(new AlreadyRegistered())
						: insertSourceDatasetVersion(tx, dataSourceIdentification, dataset)
					: insertSourceDataset(tx, dataSourceIdentification, dataset));
			
			return registerResult.thenCompose(result -> {
				return updateMetadata(tx, dataSourceIdentification, identification, dataset.getMetadata())
					.thenApply(n -> result); 
			});
		});
	}
	
	private CompletableFuture<Long> handleCleanup (final Cleanup cleanup) {
		return db.transactional (tx -> {
			final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
			
			return 
				f.sequence(
					Arrays.asList(
						tx.delete(importJob)
						.where(new SQLSubQuery().from(sourceDataset)
							.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
							.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
							.where(
								sourceDatasetVersion.id.eq(importJob.sourceDatasetVersionId)
								.and(sourceDataset.deleteTime.isNotNull())
								.and(dataset.id.isNull())
								.and(new SQLSubQuery().from(harvestNotification)
										.where(harvestNotification.sourceDatasetId.eq(sourceDataset.id)
											.and(harvestNotification.done.eq(false)))
										.notExists()))
							.exists())
						.execute(),
							
						tx.delete(sourceDatasetVersionLog)
						.where(new SQLSubQuery().from(sourceDataset)
							.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
							.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
							.where(
								sourceDatasetVersion.id.eq(sourceDatasetVersionLog.sourceDatasetVersionId)
								.and(sourceDataset.deleteTime.isNotNull())
								.and(dataset.id.isNull())
								.and(new SQLSubQuery().from(harvestNotification)
										.where(harvestNotification.sourceDatasetId.eq(sourceDataset.id)
											.and(harvestNotification.done.eq(false)))
										.notExists()))
							.exists())
						.execute(),
						
						tx.delete(sourceDatasetVersionColumn)
						.where(new SQLSubQuery().from(sourceDataset)
							.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
							.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
							.where(
								sourceDatasetVersion.id.eq(sourceDatasetVersionColumn.sourceDatasetVersionId)
								.and(sourceDataset.deleteTime.isNotNull())
								.and(dataset.id.isNull())
								.and(new SQLSubQuery().from(harvestNotification)
										.where(harvestNotification.sourceDatasetId.eq(sourceDataset.id)
											.and(harvestNotification.done.eq(false)))
										.notExists()))
							.exists())
						.execute(),
						
						tx.delete(sourceDatasetVersion)
						.where(new SQLSubQuery().from(sourceDataset)
							.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
							.where(
								sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id)
								.and(sourceDataset.deleteTime.isNotNull())
								.and(dataset.id.isNull())
								.and(new SQLSubQuery().from(harvestNotification)
									.where(harvestNotification.sourceDatasetId.eq(sourceDataset.id)
											.and(harvestNotification.done.eq(false)))
										.notExists()))
							.exists())
						.execute(),
						
						tx.delete(sourceDataset)
						.where(new SQLSubQuery().from(dataset)
							.where(dataset.sourceDatasetId.eq(sourceDataset.id)).notExists()
								.and(sourceDataset.deleteTime.isNotNull())
								.and(new SQLSubQuery().from(harvestNotification)
										.where(harvestNotification.sourceDatasetId.eq(sourceDataset.id)
											.and(harvestNotification.done.eq(false)))
										.notExists()))
						.execute(),
						
						tx.delete (category)
							.where (new SQLSubQuery ().from (sourceDatasetVersion).where (sourceDatasetVersion.categoryId.eq (category.id)).notExists ())
							.execute (),
							
						tx.update(harvestNotification)
							.set(harvestNotification.done, true)
							.where(harvestNotification.done.eq(false)
								.and(new SQLSubQuery()
									.from(sourceDataset)
									.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
									.where(sourceDataset.id.eq(harvestNotification.sourceDatasetId)
										.and(sourceDatasetVersion.id.eq(
												new SQLSubQuery()
													.from(sourceDatasetVersionSub)
													.where(sourceDatasetVersionSub.sourceDatasetId.eq(sourceDataset.id))
													.unique(sourceDatasetVersionSub.id.max())))
										.and(sourceDatasetVersion.type.ne("UNAVAILABLE")))
									.exists())
								.and(harvestNotification.notificationType.eq("SOURCE_DATASET_UNAVAILABLE")))
						.execute()
					)).thenApply(results ->
							results.stream()
								.collect(Collectors.summingLong(Long::longValue)));
		});
	}
}
