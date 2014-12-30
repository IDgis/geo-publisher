package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.Updated;
import nl.idgis.publisher.database.projections.QColumn;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.SmartFuture;

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

		db = new AsyncDatabaseHelper(database, timeout, executionContext, log);
		f = new FutureUtils(executionContext, timeout);
	}

	private <T> void returnToSender(SmartFuture<T> future) {
		future.pipeTo(getSender(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof RegisterSourceDataset) {
			returnToSender(handleRegisterSourceDataset((RegisterSourceDataset) msg));
		} else {
			unhandled(msg);
		}
	}

	private SmartFuture<Integer> getCategoryId(final AsyncHelper tx, final String identification) {
		return 
			tx.query().from(category)
				.where(category.identification.eq(identification))
				.singleResult(category.id).flatMap(id ->
					id == null
						? tx.insert(category)
							.set(category.identification, identification)
							.set(category.name, identification)
							.executeWithKey(category.id)
						: f.successful(id));
	}
	
	private SmartFuture<VectorDataset> getSourceDatasetVersion(final AsyncHelper tx, final Integer versionId) {
		log.debug("retrieving source dataset version");
		
		return f
			.collect(
				tx.query().from(sourceDatasetVersion)
					.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
					.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))			
					.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
					.where(sourceDatasetVersion.id.eq(versionId))
					.singleResult(
						sourceDataset.identification,
						sourceDatasetVersion.name,
						category.identification,
						sourceDatasetVersion.revision))
			.collect(					
				tx.query().from(sourceDatasetVersionColumn)
					.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(versionId))
					.orderBy(sourceDatasetVersionColumn.index.asc())
					.list(new QColumn(
						sourceDatasetVersionColumn.name,
						sourceDatasetVersionColumn.dataType)))
			
			.map((baseInfo, columnInfo) -> 
				new VectorDataset(
					baseInfo.get(sourceDataset.identification), 
					baseInfo.get(category.identification), 
					baseInfo.get(sourceDatasetVersion.revision), 
					Collections.<Log>emptySet(), 
					new Table(
						baseInfo.get(sourceDatasetVersion.name), 
						columnInfo.list())));
	}
	
	private SmartFuture<Optional<VectorDataset>> getCurrentSourceDatasetVersion(final AsyncHelper tx, final String dataSourceIdentification, final String identification) {
		log.debug("get current source dataset version");
		
		return
			tx.query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
						.and(sourceDataset.identification.eq(identification)))
				.singleResult(sourceDatasetVersion.id.max()).flatMap(maxVersionId -> 
					maxVersionId == null
						? f.successful(Optional.empty())
						: getSourceDatasetVersion(tx, maxVersionId).map(dataset -> Optional.of(dataset)));
	}
	
	private SmartFuture<Object> insertSourceDatasetVersion(AsyncHelper tx, String dataSourceIdentification, VectorDataset dataset) {
		log.debug("inserting source dataset (by dataSource identification)");
		
		return 
			tx.query().from(sourceDataset)
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
					.and(sourceDataset.identification.eq(dataset.getId())))
				.singleResult(sourceDataset.id).flatMap(sourceDatasetId -> 
					insertSourceDatasetVersion(tx, sourceDatasetId, dataset)).mapValue(new Updated());
	}
	
	private SmartFuture<Void> insertSourceDatasetVersion(AsyncHelper tx, Integer sourceDatasetId, VectorDataset dataset) {
		log.debug("inserting source dataset (by id)");
		
		Table table = dataset.getTable();
		
		return 
			getCategoryId(tx, dataset.getCategoryId()).flatMap(categoryId -> 					
				tx.insert(sourceDatasetVersion)
					.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
					.set(sourceDatasetVersion.name, table.getName())
					.set(sourceDatasetVersion.categoryId, categoryId)
					.set(sourceDatasetVersion.revision, new Timestamp(dataset.getRevisionDate().getTime()))
					.executeWithKey(sourceDatasetVersion.id)).flatMap(sourceDatasetVersionId -> {
						AtomicInteger index = new AtomicInteger();
						return 
							table.getColumns().stream().map((Column column) ->
								tx
									.insert(sourceDatasetVersionColumn)
									.set(sourceDatasetVersionColumn.sourceDatasetVersionId, sourceDatasetVersionId)
									.set(sourceDatasetVersionColumn.index, index.getAndIncrement())
									.set(sourceDatasetVersionColumn.name, column.getName())
									.set(sourceDatasetVersionColumn.dataType, column.getDataType().toString())
									.execute()).reduce(f.successful(0l), (a, b) -> a.flatMap(l -> b)).mapValue(null);
		});
	}
	
	private SmartFuture<Object> insertSourceDataset(AsyncHelper tx, String dataSourceIdentification, VectorDataset dataset) {
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
					.executeWithKey(sourceDataset.id).flatMap(sourceDatasetId -> 
						insertSourceDatasetVersion(tx, sourceDatasetId, dataset)).mapValue(new Registered());
					
				
	}

	private SmartFuture<Object> handleRegisterSourceDataset(final RegisterSourceDataset msg) {
		log.debug("registering source dataset");
		
		VectorDataset dataset = msg.getDataset();
		String dataSource = msg.getDataSource();
		
		return db.transactional(tx ->			
			getCurrentSourceDatasetVersion(tx, dataSource, dataset.getId()).flatMap(currentVersion -> 
				currentVersion.isPresent()
					? currentVersion.get().equals(dataset)
						? f.successful(new AlreadyRegistered())
						: insertSourceDatasetVersion(tx, dataSource, dataset)
					: insertSourceDataset(tx, dataSource, dataset)));
	}

}
