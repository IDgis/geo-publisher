package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QSourceDatasetVersionLog.sourceDatasetVersionLog;
import static nl.idgis.publisher.utils.StreamUtils.index;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.projections.QColumn;

import nl.idgis.publisher.dataset.messages.AlreadyRegistered;
import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;
import nl.idgis.publisher.protocol.messages.Failure;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.DatasetLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.UnavailableDataset;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.JsonUtils;

public class DatasetManager extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ActorRef database;

	private AsyncDatabaseHelper db;

	private FutureUtils f;

	public DatasetManager(ActorRef database) {
		this.database = database;
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
		} else {
			unhandled(msg);
		}
	}

	private CompletableFuture<Integer> getCategoryId(final AsyncHelper tx, final String identification) {
		log.debug("get category id: {}", identification);
		
		// category can be null for unavailable datasets
		if(identification == null) {
			return f.successful(null);
		}
		
		return
			tx.query().from(category)
				.where(category.identification.eq(identification))
				.singleResult(category.id).thenCompose(id -> {
					if(id.isPresent()) {
						return f.successful(id.get());
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
					.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))			
					.leftJoin(category).on(category.id.eq(sourceDatasetVersion.categoryId))
					.where(sourceDatasetVersion.id.eq(versionId))
					.singleResult(
						sourceDataset.identification,
						sourceDatasetVersion.name,
						sourceDatasetVersion.alternateTitle,
						sourceDatasetVersion.type,
						category.identification,
						sourceDatasetVersion.revision))
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
						
						String id = baseInfo.get(sourceDataset.identification);
						String name = baseInfo.get(sourceDatasetVersion.name);
						String alternateTitle = baseInfo.get(sourceDatasetVersion.alternateTitle);
						String type = baseInfo.get(sourceDatasetVersion.type);
						String categoryId = baseInfo.get(category.identification);
						Date revisionDate = baseInfo.get(sourceDatasetVersion.revision);
						
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
									new Table(columnInfo.list()));
								break;
							default:
								dataset = new UnavailableDataset(	
									id,
									name,
									alternateTitle,
									categoryId,
									revisionDate,
									logs);
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
						.and(sourceDataset.identification.eq(identification)))
				.singleResult(sourceDatasetVersion.id.max()).thenCompose(maxVersionId -> {
					if(maxVersionId.isPresent()) {
						return getSourceDatasetVersion(tx, maxVersionId.get()).thenApply(Optional::of);						
					} else {
						return f.successful(Optional.empty());
					}					

				}); 
						
	}
	
	private CompletableFuture<Object> insertSourceDatasetVersion(AsyncHelper tx, String dataSourceIdentification, Dataset dataset) {
		log.debug("inserting source dataset (by dataSource identification)");
		
		return 
			tx.query().from(sourceDataset)
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
					.and(sourceDataset.identification.eq(dataset.getId())))
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
			
			return tx.insert(sourceDatasetVersion)
				.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
				.set(sourceDatasetVersion.type, type)
				.set(sourceDatasetVersion.name, name)		
				.set(sourceDatasetVersion.alternateTitle, alternateTitle)
				.set(sourceDatasetVersion.categoryId, categoryId)
				.set(sourceDatasetVersion.revision, revision)
				.executeWithKey(sourceDatasetVersion.id);
			}).thenCompose(sourceDatasetVersionId -> {
				log.debug("sourceDatasetVersionId: {}", sourceDatasetVersionId);
				
				return insertSourceDatasetVersionLogs(tx, sourceDatasetVersionId, dataset).thenCompose(v -> {					
					if(dataset instanceof VectorDataset) {
						return insertSourceDatasetVersionColumns(tx, sourceDatasetVersionId, (VectorDataset)dataset);
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
					sourceDataset.identification)
				.select(new SQLSubQuery()
					.from(dataSource)
					.where(dataSource.identification.eq(dataSourceIdentification))
					.list(dataSource.id, dataset.getId()))
				.executeWithKey(sourceDataset.id).thenCompose(sourceDatasetId -> 
					insertSourceDatasetVersion(tx, sourceDatasetId, dataset)).thenApply(v -> new Registered());
	}

	private CompletableFuture<Object> handleRegisterSourceDataset(final RegisterSourceDataset msg) {
		log.debug("registering source dataset");
		
		Dataset dataset = msg.getDataset();
		String dataSource = msg.getDataSource();
		
		return db.transactional(tx ->			
			getCurrentSourceDatasetVersion(tx, dataSource, dataset.getId()).thenCompose(currentVersion -> 
				currentVersion.isPresent()
					? currentVersion.get().equals(dataset)
						? f.successful(new AlreadyRegistered())
						: insertSourceDatasetVersion(tx, dataSource, dataset)
					: insertSourceDataset(tx, dataSource, dataset)));
	}

}
