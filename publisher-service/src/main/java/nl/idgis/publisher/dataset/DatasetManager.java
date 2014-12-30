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

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

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
import nl.idgis.publisher.utils.TypedList;

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

	private <T> void returnToSender(Future<T> future) {
		Patterns.pipe(future, getContext().dispatcher()).pipeTo(getSender(),
				getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof RegisterSourceDataset) {
			returnToSender(handleRegisterSourceDataset((RegisterSourceDataset) msg));
		} else {
			unhandled(msg);
		}
	}

	private Future<Integer> getCategoryId(final AsyncHelper tx, final String identification) {
		return f.flatMap(
			tx.query().from(category)
				.where(category.identification.eq(identification))
				.singleResult(category.id),

			(Integer id) -> 
				id == null
					? tx.insert(category)
						.set(category.identification, identification)
						.set(category.name, identification)
						.executeWithKey(category.id)
					: Futures.successful(id));
	}
	
	private Future<VectorDataset> getSourceDatasetVersion(final AsyncHelper tx, final Integer versionId) {
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
			
			.map((Tuple baseInfo, TypedList<Column> columnInfo) -> 
				new VectorDataset(
					baseInfo.get(sourceDataset.identification), 
					baseInfo.get(category.identification), 
					baseInfo.get(sourceDatasetVersion.revision), 
					Collections.<Log>emptySet(), 
					new Table(
						baseInfo.get(sourceDatasetVersion.name), 
						columnInfo.list())));
	}
	
	private Future<Optional<VectorDataset>> getCurrentSourceDatasetVersion(final AsyncHelper tx, final String dataSourceIdentification, final String identification) {
		log.debug("get current source dataset version");
		
		return f.flatMap(
			tx.query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.where(dataSource.identification.eq(dataSourceIdentification)
						.and(sourceDataset.identification.eq(identification)))
				.singleResult(sourceDatasetVersion.id.max()),
				
			(Integer maxVersionId) -> 
				maxVersionId == null
					? Futures.successful(Optional.empty())
					: f.map(
						getSourceDatasetVersion(tx, maxVersionId),
						
						(VectorDataset dataset) -> Optional.of(dataset)));
	}
	
	private Future<Object> insertSourceDatasetVersion(AsyncHelper tx, String dataSourceIdentification, VectorDataset dataset) {
		log.debug("inserting source dataset (by dataSource identification)");
		
		return 
			f.mapValue(
				f.flatMap(
					tx.query().from(sourceDataset)
						.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
						.where(dataSource.identification.eq(dataSourceIdentification)
							.and(sourceDataset.identification.eq(dataset.getId())))
						.singleResult(sourceDataset.id),
						
					(Integer sourceDatasetId) -> insertSourceDatasetVersion(tx, sourceDatasetId, dataset)),
				
				new Updated());
	}
	
	private Future<Void> insertSourceDatasetVersion(AsyncHelper tx, Integer sourceDatasetId, VectorDataset dataset) {
		log.debug("inserting source dataset (by id)");
		
		Table table = dataset.getTable();
		
		return f.flatMap(
			f.flatMap(
				getCategoryId(tx, dataset.getCategoryId()),
				
				(Integer categoryId) -> 					
					tx.insert(sourceDatasetVersion)
						.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
						.set(sourceDatasetVersion.name, table.getName())
						.set(sourceDatasetVersion.categoryId, categoryId)
						.set(sourceDatasetVersion.revision, new Timestamp(dataset.getRevisionDate().getTime()))
						.executeWithKey(sourceDatasetVersion.id)),
			
			(Integer sourceDatasetVersionId) -> {
				AtomicInteger index = new AtomicInteger();
				return f.mapValue(
					table.getColumns().stream().map((Column column) ->
						tx
							.insert(sourceDatasetVersionColumn)
							.set(sourceDatasetVersionColumn.sourceDatasetVersionId, sourceDatasetVersionId)
							.set(sourceDatasetVersionColumn.index, index.getAndIncrement())
							.set(sourceDatasetVersionColumn.name, column.getName())
							.set(sourceDatasetVersionColumn.dataType, column.getDataType().toString())
							.execute()).reduce(Futures.successful(0l), (Future<Long> a, Future<Long> b) -> 
								f.flatMap(a, (Long l) -> 
									b)),
								
					(Void)null);
			});
	}
	
	private Future<Object> insertSourceDataset(AsyncHelper tx, String dataSourceIdentification, VectorDataset dataset) {
		log.debug("inserting source dataset");
		
		return
			f.mapValue(
				f.flatMap(
					tx.insert(sourceDataset)
						.columns(
							sourceDataset.dataSourceId, 
							sourceDataset.identification)
						.select(new SQLSubQuery()
							.from(dataSource)
							.where(dataSource.identification.eq(dataSourceIdentification))
							.list(dataSource.id, dataset.getId()))
						.executeWithKey(sourceDataset.id),
						
					(Integer sourceDatasetId) -> insertSourceDatasetVersion(tx, sourceDatasetId, dataset)),
					
				new Registered());
	}

	private Future<Object> handleRegisterSourceDataset(final RegisterSourceDataset msg) {
		log.debug("registering source dataset");
		
		VectorDataset dataset = msg.getDataset();
		String dataSource = msg.getDataSource();
		
		return db.transactional((AsyncHelper tx) ->
			f.flatMap(
				getCurrentSourceDatasetVersion(tx, dataSource, dataset.getId()),
				
				(Optional<VectorDataset> currentVersion) -> 
					currentVersion.isPresent()
						? currentVersion.get().equals(dataset)
							? Futures.successful(new AlreadyRegistered())
							: insertSourceDatasetVersion(tx, dataSource, dataset)
						: insertSourceDataset(tx, dataSource, dataset)));
	}

}
