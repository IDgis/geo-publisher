package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.utils.StreamUtils.index;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLSubQuery;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import scala.concurrent.ExecutionContext;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.Updated;
import nl.idgis.publisher.database.projections.QColumn;

import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.utils.FutureUtils;

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
		Timeout timeout = Timeout.apply(15000);
		ExecutionContext executionContext = getContext().dispatcher();

		f = new FutureUtils(executionContext, timeout);
		db = new AsyncDatabaseHelper(database, f, log);		
	}

	private <T> void returnToSender(CompletableFuture<T> future) {
		ActorRef sender = getSender();
		future.thenAccept(t -> sender.tell(t, getSelf()));
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
		return 
			tx.query().from(category)
				.where(category.identification.eq(identification))
				.singleResult(category.id).thenCompose(id ->
					id == null
						? tx.insert(category)
							.set(category.identification, identification)
							.set(category.name, identification)
							.executeWithKey(category.id)
						: f.successful(id));
	}
	
	private CompletableFuture<VectorDataset> getSourceDatasetVersion(final AsyncHelper tx, final Integer versionId) {
		log.debug("retrieving source dataset version");
		
		return			
			tx.query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))			
				.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
				.where(sourceDatasetVersion.id.eq(versionId))
				.singleResult(
					sourceDataset.identification,
					sourceDatasetVersion.name,
					category.identification,
					sourceDatasetVersion.revision)
			
			.thenCombine(
				tx.query().from(sourceDatasetVersionColumn)
				.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(versionId))
				.orderBy(sourceDatasetVersionColumn.index.asc())
				.list(new QColumn(
					sourceDatasetVersionColumn.name,
					sourceDatasetVersionColumn.dataType)),
			
			(baseInfo, columnInfo) -> 
				new VectorDataset(
					baseInfo.get(sourceDataset.identification), 
					baseInfo.get(category.identification), 
					baseInfo.get(sourceDatasetVersion.revision), 
					Collections.<Log>emptySet(), 
					new Table(
						baseInfo.get(sourceDatasetVersion.name), 
						columnInfo.list())));
	}
	
	private CompletableFuture<Optional<VectorDataset>> getCurrentSourceDatasetVersion(final AsyncHelper tx, final String dataSourceIdentification, final String identification) {
		log.debug("get current source dataset version");
		
		return
			tx.query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
						.and(sourceDataset.identification.eq(identification)))
				.singleResult(sourceDatasetVersion.id.max()).thenCompose(maxVersionId -> 
					maxVersionId == null
						? f.successful(Optional.empty())
						: getSourceDatasetVersion(tx, maxVersionId).thenApply(dataset -> Optional.of(dataset)));
	}
	
	private CompletableFuture<Object> insertSourceDatasetVersion(AsyncHelper tx, String dataSourceIdentification, VectorDataset dataset) {
		log.debug("inserting source dataset (by dataSource identification)");
		
		return 
			tx.query().from(sourceDataset)
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
					.and(sourceDataset.identification.eq(dataset.getId())))
				.singleResult(sourceDataset.id).thenCompose(sourceDatasetId -> 
					insertSourceDatasetVersion(tx, sourceDatasetId, dataset)).thenApply(v -> new Updated());
	}
	
	private CompletableFuture<Void> insertSourceDatasetVersion(AsyncHelper tx, Integer sourceDatasetId, VectorDataset dataset) {
		log.debug("inserting source dataset (by id)");
		
		Table table = dataset.getTable();
		
		return 
			getCategoryId(tx, dataset.getCategoryId()).thenCompose(categoryId ->
				tx.insert(sourceDatasetVersion)
					.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
					.set(sourceDatasetVersion.name, table.getName())
					.set(sourceDatasetVersion.categoryId, categoryId)
					.set(sourceDatasetVersion.revision, new Timestamp(dataset.getRevisionDate().getTime()))
					.executeWithKey(sourceDatasetVersion.id)).thenCompose(sourceDatasetVersionId ->
						index(table.getColumns().stream())
							.map(indexedColumn -> {
								Column column = indexedColumn.getValue();
								
								return tx.insert(sourceDatasetVersionColumn)
									.set(sourceDatasetVersionColumn.sourceDatasetVersionId, sourceDatasetVersionId)
									.set(sourceDatasetVersionColumn.index, indexedColumn.getIndex())
									.set(sourceDatasetVersionColumn.name, column.getName())
									.set(sourceDatasetVersionColumn.dataType, column.getDataType().toString())
									.execute();
							})
							.reduce(f.successful(null), (a, b) -> a.thenCompose(t -> b))
							.thenApply(l -> null));
	}
	
	private CompletableFuture<Object> insertSourceDataset(AsyncHelper tx, String dataSourceIdentification, VectorDataset dataset) {
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
		
		VectorDataset dataset = msg.getDataset();
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
