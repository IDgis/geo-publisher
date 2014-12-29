package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;

import java.sql.Timestamp;
import java.util.ArrayList;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.util.Timeout;

import scala.concurrent.Future;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.Updated;
import nl.idgis.publisher.database.projections.QColumn;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.utils.TypedList;

public class DatasetManager extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
	private final ActorRef database;
	
	private AsyncDatabaseHelper async;
	
	public DatasetManager(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(DatasetManager.class, database);
	}
	
	@Override
	public void preStart() throws Exception {
		async = new AsyncDatabaseHelper(database, Timeout.apply(15000), getContext().dispatcher(), log);
	}
	
	private <T> void returnToSender(Future<T> future) {
		Patterns.pipe(future, getContext().dispatcher())
			.pipeTo(getSender(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof RegisterSourceDataset) {
			returnToSender(handleRegisterSourceDataset((RegisterSourceDataset)msg));
		} else {		
			unhandled(msg);
		}
	}
	
	private Future<Integer> getCategoryId(final String identification) {
		return async.collect(						
			async.query().from(category)
				.where(category.identification.eq(identification))
				.singleResult(category.id))
				
		.flatResult(new AbstractFunction1<Integer, Future<Integer>>() {

			@Override
			public Future<Integer> apply(Integer id) {
				if(id == null) {
					return async.insert(category)
						.set(category.identification, identification)
						.set(category.name, identification)
						.executeWithKey(category.id);
				} else {
					return Futures.successful(id);
				}
			}
			
		})
		
		.returnValue();
	}

	private Future<Object> handleRegisterSourceDataset(final RegisterSourceDataset rsd) {
		final VectorDataset dataset = rsd.getDataset();
		final Timestamp revision = new Timestamp(dataset.getRevisionDate().getTime());
		final Table table = dataset.getTable();
		
		return async.transactional(new Function<AsyncHelper, Future<Object>>() {

			@Override
			public Future<Object> apply(final AsyncHelper async) throws Exception {
				return async.collect(				
					async.query().from(sourceDatasetVersion)
						.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
						.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
						.where(dataSource.identification.eq(rsd.getDataSource())
							.and(sourceDataset.identification.eq(dataset.getId())))
						.singleResult(sourceDatasetVersion.id.max()))
						
					.flatResult(new AbstractFunction1<Integer, Future<Object>>() {					
						
						private Future<Object> insertSourceDatasetVersion(Future<Integer> sourceDatasetId, final Object result) {
							return async
								.collect(sourceDatasetId)
								.collect(getCategoryId(dataset.getCategoryId()))
								
							.flatResult(new AbstractFunction2<Integer, Integer, Future<Object>>() {

								@Override
								public Future<Object> apply(Integer sourceDatasetId, Integer categoryId) {
									return async.collect(								
										async.insert(sourceDatasetVersion)
											.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
											.set(sourceDatasetVersion.name, table.getName())
											.set(sourceDatasetVersion.categoryId, categoryId)
											.set(sourceDatasetVersion.revision, new Timestamp(dataset.getRevisionDate().getTime()))
											.executeWithKey(sourceDatasetVersion.id))
									.flatResult(new AbstractFunction1<Integer, Future<Object>>() {

										@Override
										public Future<Object> apply(Integer versionId) {
											int i = 0;
											
											ArrayList<Future<Long>> columns = new ArrayList<>();
											for(Column column : table.getColumns()) {
												columns.add(
													async.insert(sourceDatasetVersionColumn)
														.set(sourceDatasetVersionColumn.sourceDatasetVersionId, versionId)
														.set(sourceDatasetVersionColumn.index, i++)
														.set(sourceDatasetVersionColumn.name, column.getName())
														.set(sourceDatasetVersionColumn.dataType, column.getDataType().toString())
														.execute());
											}
											
											return async.collect(
													Futures.sequence(columns, getContext().dispatcher()))
													
											.flatResult(new AbstractFunction1<Iterable<Long>, Future<Object>>() {

												@Override
												public Future<Object> apply(Iterable<Long> i) {
													return Futures.successful(result);
												}
											})
											
											.returnValue();
										}
										
									})
									
									.returnValue();
								}
								
							})
							
							.returnValue();						
						}

						@Override
						public Future<Object> apply(final Integer versionId) {
							if(versionId == null) { // new dataset
								return insertSourceDatasetVersion(
									async.insert(sourceDataset)
										.columns(
											sourceDataset.dataSourceId,
											sourceDataset.identification)
										.select(
											new SQLSubQuery().from(dataSource)
												.list(
													dataSource.id,
													dataset.getId()))
									.executeWithKey(sourceDataset.id), new Registered());
							} else { // existing dataset
								return async.collect(
									async.query().from(sourceDataset)
										.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
										.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(versionId))
										.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
										.where(dataSource.identification.eq(rsd.getDataSource())
											.and(sourceDataset.identification.eq(dataset.getId())))
										.singleResult(
											sourceDataset.id,
											sourceDatasetVersion.name,
											category.identification,
											sourceDatasetVersion.revision,
											sourceDataset.deleteTime))
											
								.flatResult(new AbstractFunction1<Tuple, Future<Object>>() {

									@Override
									public Future<Object> apply(Tuple existing) {
										final Integer sourceDatasetId = existing.get(sourceDataset.id);
										
										final String existingName = existing.get(sourceDatasetVersion.name);
										final String existingCategoryIdentification = existing.get(category.identification);
										final Timestamp existingRevision = existing.get(sourceDatasetVersion.revision);
										final Timestamp existingDeleteTime = existing.get(sourceDataset.deleteTime);
										
										return async.collect(
											async.query().from(sourceDatasetVersionColumn)
												.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(versionId))
												.orderBy(sourceDatasetVersionColumn.index.asc())
												.list(new QColumn(sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.dataType)))
										
										.flatResult(new AbstractFunction1<TypedList<Column>, Future<Object>>() {

											@Override
											public Future<Object> apply(final TypedList<Column> existingColumns) {
												if(existingName.equals(table.getName()) // still identical
														&& existingCategoryIdentification.equals(dataset.getCategoryId())
														&& existingRevision.equals(revision)
														&& existingDeleteTime == null
														&& existingColumns.equals(new TypedList<Column>(Column.class, table.getColumns()))) {
													
													return Futures.<Object>successful(new AlreadyRegistered());													
												} else {
													if(existingDeleteTime != null) { // reviving dataset
														return async.collect(
															async.update(sourceDataset)															
																.setNull(sourceDataset.deleteTime)						
																.execute())
																
														.result(new AbstractFunction1<Long, Object>() {

															@Override
															public Object apply(Long l) {
																return insertSourceDatasetVersion(Futures.successful(sourceDatasetId), new Updated());
															}															
														})
														
														.returnValue();
													} else {
														return insertSourceDatasetVersion(Futures.successful(sourceDatasetId), new Updated());
													}
												}
											}
											
										})
										
										.returnValue();
									}
									
								})
								
								.returnValue();
							}
						}
						
					})
					
					.returnValue();
			}
			
		});
	}

}
