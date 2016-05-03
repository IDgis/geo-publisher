package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QImportJobColumn.importJobColumn;
import static nl.idgis.publisher.database.QDatasetView.datasetView;
import static nl.idgis.publisher.database.QDatasetCopy.datasetCopy;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QSourceDatasetVersionLog.sourceDatasetVersionLog;
import static nl.idgis.publisher.utils.StreamUtils.index;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.messages.CopyTable;
import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.CreateView;
import nl.idgis.publisher.database.messages.DropTable;
import nl.idgis.publisher.database.messages.DropView;
import nl.idgis.publisher.database.projections.QColumn;

import nl.idgis.publisher.dataset.messages.AlreadyRegistered;
import nl.idgis.publisher.dataset.messages.Cleanup;
import nl.idgis.publisher.dataset.messages.DeleteSourceDatasets;
import nl.idgis.publisher.dataset.messages.PrepareTable;
import nl.idgis.publisher.dataset.messages.PrepareView;
import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.DatasetLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.service.UnavailableDataset;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.domain.service.RasterDataset;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.JsonUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.expr.DateTimeExpression;
import com.mysema.query.types.query.ListSubQuery;

public class DatasetManager extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ActorRef database;
	
	private final MetadataDocumentFactory mdf;

	private AsyncDatabaseHelper db;

	private FutureUtils f;

	public DatasetManager(ActorRef database) throws Exception {
		this.database = database;
		
		mdf = new MetadataDocumentFactory();
	}

	public static Props props(ActorRef database) {
		return Props.create(DatasetManager.class, database);
	}

	@Override
	public void preStart() throws Exception {
		log.debug("start");
		
		Timeout timeout = Timeout.apply(15000);

		f = new FutureUtils(getContext(), timeout);
		db = new AsyncDatabaseHelper(database, f, log);		
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
	
	private CompletableFuture<Object> makeDatasetCopy(AsyncHelper tx, String datasetId, List<Column> columns) {
		log.debug("making dataset copy");
		
		return tx.ask(new DropView("data", datasetId)).thenCompose(dropViewResult ->
			dropViewResult instanceof Ack
				? tx.delete(datasetView)
					.where(new SQLSubQuery().from(dataset)
						.where(dataset.identification.eq(datasetId))
						.where(dataset.id.eq(datasetView.datasetId))
						.exists())
					.execute()
						.thenCompose(cnt ->
							tx.ask(new CopyTable("data", datasetId, "staging_data", datasetId))).thenCompose(copyTableResult ->
								tx.insert(datasetCopy)
									.columns(
										datasetCopy.datasetId,
										datasetCopy.index,
										datasetCopy.name,
										datasetCopy.dataType)
									.select(subselectDatasetColumns(datasetId))
									.execute().thenCompose(cnt -> 
										tx.ask(new CreateTable("staging_data", datasetId, columns))))
				: f.successful(dropViewResult));
	}
	
	private CompletableFuture<Object> keepDatasetView(AsyncHelper tx, String datasetId, List<Column> columns) {
		log.debug("keeping dataset view");
		
		return tx.ask(new DropView("data", datasetId)).thenCompose(dropViewResult ->
			dropViewResult instanceof Ack
				? tx.ask(new CreateTable("staging_data", datasetId, columns)).thenCompose(createTableResult ->
					createTableResult instanceof Ack
						? tx.ask(new CreateView("data", datasetId, "staging_data", datasetId))
						: f.successful(createTableResult))
				: f.successful(dropViewResult));
	}

	private CompletableFuture<Object> handlePrepareTable(PrepareTable msg) {
		log.debug("preparing table: {}", msg);
		
		String datasetId = msg.getDatasetId();
		List<Column> columns = 
			Stream
				.concat(
					msg.getColumns().stream(), 
					Stream.of(new Column(datasetId + "_id", Type.SERIAL)))
				.collect(Collectors.toList());
		
		return db.transactional(msg, tx ->
			fetchViewInfo(tx, datasetId).thenCompose(viewColumns ->
				viewColumns.isEmpty() // view present?
					? tx.ask(new CreateTable("staging_data", datasetId, columns)) 
					: viewColumns.equals(columns) // same columns?
						? keepDatasetView(tx, datasetId, columns)
						: makeDatasetCopy(tx, datasetId, columns)));
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
							t.get(datasetView.dataType)))
						.collect(Collectors.toList()));
	}

	private CompletableFuture<Long> handleDeleteSourceDatasets(DeleteSourceDatasets msg) {
		return
			db.update(sourceDataset)
			.set(sourceDataset.deleteTime, DateTimeExpression.currentTimestamp(Timestamp.class))
			.where(sourceDataset.externalIdentification.in(msg.getDatasetIds()))
			.execute();
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
						sourceDatasetVersion.confidential))
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
					sourceDatasetVersionColumn.dataType))).thenApply((baseInfoOptional, logInfo, columnInfo) -> {
						Tuple baseInfo = baseInfoOptional.orElseThrow(() -> new IllegalArgumentException("source dataset version missing"));
						
						String id = baseInfo.get(sourceDataset.externalIdentification);
						String name = baseInfo.get(sourceDatasetVersion.name);
						String alternateTitle = baseInfo.get(sourceDatasetVersion.alternateTitle);
						String type = baseInfo.get(sourceDatasetVersion.type);
						String categoryId = baseInfo.get(category.identification);
						Date revisionDate = baseInfo.get(sourceDatasetVersion.revision);
						boolean confidential = baseInfo.get(sourceDatasetVersion.confidential);
						byte[] metadataContent = baseInfo.get(sourceDatasetMetadata.document);
						
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
						
						// convert from timestamp to date because harvester provides date objects,
						// otherwise source dataset versions are never equal.
						if(revisionDate != null) {							
							revisionDate = new Date(revisionDate.getTime());
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
									metadata,
									new Table(columnInfo.list()));
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
									metadata);
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
									metadata);
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
					
					return insertSourceDatasetVersion(tx, sourceDatasetId, dataset).thenApply(v -> new Updated());
				});
	}
	
	private CompletableFuture<Void> insertSourceDatasetVersion(AsyncHelper tx, Integer sourceDatasetId, Dataset dataset) {
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
			Date revisionDate = dataset.getRevisionDate();
			if(revisionDate != null) {
				revision = new Timestamp(revisionDate.getTime());
			} else {
				revision = null;
			}
			
			String name = dataset.getName();
			String alternateTitle = dataset.getAlternateTitle();
			boolean confidential = dataset.isConfidential();
			
			return tx.insert(sourceDatasetVersion)
				.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
				.set(sourceDatasetVersion.type, type)
				.set(sourceDatasetVersion.name, name)		
				.set(sourceDatasetVersion.alternateTitle, alternateTitle)
				.set(sourceDatasetVersion.categoryId, categoryId.orElse(null))
				.set(sourceDatasetVersion.revision, revision)
				.set(sourceDatasetVersion.confidential, confidential)
				.executeWithKey(sourceDatasetVersion.id);
			}).thenCompose(sourceDatasetVersionId -> {
				log.debug("sourceDatasetVersionId: {}", sourceDatasetVersionId);
				
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
					sourceDataset.identification)
				.select(new SQLSubQuery()
					.from(dataSource)
					.where(dataSource.identification.eq(dataSourceIdentification))
					.list(
						dataSource.id, 
						dataset.getId(),
						UUID.randomUUID().toString()))
				.executeWithKey(sourceDataset.id).thenCompose(sourceDatasetId -> 
					insertSourceDatasetVersion(tx, sourceDatasetId.get(), dataset)).thenApply(v -> new Registered());
	}
	
	private CompletableFuture<Long> ensureNotDeleted(AsyncHelper tx, String datasetId) {
		return 
			tx.update(sourceDataset)
			.setNull(sourceDataset.deleteTime)
			.where(sourceDataset.externalIdentification.eq(datasetId))
			.execute();
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
								.execute().thenApply(insertResult -> null);
							} else {
								log.debug("metadata document found");
								try {
									MessageDigest md = MessageDigest.getInstance("MD5");
									if(Arrays.equals(md.digest(newDocument), md.digest(currentDocument))) {
										log.debug("same hash -> no nothing");
										return f.successful(null);
									}
									
									log.debug("different hash -> update");
									return tx.update(sourceDatasetMetadata)
									.set(sourceDatasetMetadata.document, newDocument)
									.where(sourceDatasetMetadata.sourceDatasetId.eq(id))
									.execute().thenApply(updateResult -> null);
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
			return 
				f.sequence(
					Arrays.asList(
						tx.delete(sourceDatasetVersionLog)
						.where(new SQLSubQuery().from(sourceDataset)
							.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
							.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
							.where(
								sourceDatasetVersion.id.eq(sourceDatasetVersionLog.sourceDatasetVersionId)
								.and(sourceDataset.deleteTime.isNotNull())
								.and(dataset.id.isNull()))
							.exists())
						.execute(),
						
						tx.delete(sourceDatasetVersionColumn)
						.where(new SQLSubQuery().from(sourceDataset)
							.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
							.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
							.where(
								sourceDatasetVersion.id.eq(sourceDatasetVersionColumn.sourceDatasetVersionId)
								.and(sourceDataset.deleteTime.isNotNull())
								.and(dataset.id.isNull()))
							.exists())
						.execute(),
						
						tx.delete(sourceDatasetVersion)
						.where(new SQLSubQuery().from(sourceDataset)							
							.leftJoin(dataset).on(dataset.sourceDatasetId.eq(sourceDataset.id))
							.where(
								sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id)
								.and(sourceDataset.deleteTime.isNotNull())
								.and(dataset.id.isNull()))
							.exists())
						.execute(),
						
						tx.delete(sourceDataset)
						.where(new SQLSubQuery().from(dataset)
							.where(dataset.sourceDatasetId.eq(sourceDataset.id))
							.notExists().and(sourceDataset.deleteTime.isNotNull()))
						.execute(),
						
						tx.delete (category)
							.where (new SQLSubQuery ().from (sourceDatasetVersion).where (sourceDatasetVersion.categoryId.eq (category.id)).notExists ())
							.execute ()
					)).thenApply(results ->
							results.stream()
								.collect(Collectors.summingLong(Long::longValue)));
		});
	}
}
