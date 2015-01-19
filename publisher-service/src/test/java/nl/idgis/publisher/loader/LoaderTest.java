package nl.idgis.publisher.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.AbstractServiceTest;

import nl.idgis.publisher.database.messages.AddNotificationResult;
import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.GetJobLog;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.StoredJobLog;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.database.messages.UpdateDataset;
import nl.idgis.publisher.database.messages.UpdateJobState;

import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.load.ImportLogType;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.job.load.MissingColumnsLog;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Filter.OperatorExpression;
import nl.idgis.publisher.domain.web.Filter.ColumnReferenceExpression;
import nl.idgis.publisher.domain.web.Filter.ValueExpression;
import nl.idgis.publisher.domain.web.Filter.OperatorType;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.job.messages.CreateImportJob;
import nl.idgis.publisher.job.messages.GetImportJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.TypedIterable;

public class LoaderTest extends AbstractServiceTest {
	
	static class SetInsertCount {
		
		final int count;
		
		SetInsertCount(int count) {
			this.count = count;
		}
		
	}
	
	static class GetInsertCount {
		
	}
	
	static class TransactionMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		int insertCount = 0;

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);			
					
			if(msg instanceof InsertRecord) {
				insertCount++;
			} else if(msg instanceof Commit) {
				getContext().parent().tell(new SetInsertCount(insertCount), getSelf());
				getContext().stop(getSelf());
			}
		
			getSender().tell(new Ack(), getSelf());
		}
		
	}
	
	static class GeometryDatabaseMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		ActorRef sender;
		Integer insertCount;

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof SetInsertCount) {
				insertCount = ((SetInsertCount) msg).count;
				sendInsertCount();
			} else if(msg instanceof GetInsertCount) {
				sender = getSender();
				sendInsertCount();
			} else if(msg instanceof StartTransaction) {
				ActorRef transaction = getContext().actorOf(Props.create(TransactionMock.class));				
				getSender().tell(new TransactionCreated(transaction), getSelf());
			} else {
				unhandled(msg);
			}
		}

		private void sendInsertCount() {
			if(sender != null && insertCount != null) {
				sender.tell(insertCount, getSelf());
				
				sender = null;
				insertCount = null;
			}			
		}	
		
		
	}
	
	static class GetColumns {
		
	}
	
	static class DataSourceMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		ActorRef sender = null;
		List<String> columns = null;

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof GetDataset) {
				GetDataset gd = (GetDataset)msg;
				
				columns = gd.getColumns();
				sendColumns();
				
				ActorRef receiver = getContext().actorOf(gd.getReceiverProps(), "receiver");
				receiver.tell(new StartImport(getSender(), 10), getSelf());
				getContext().become(waitingForStart(), true);
			} else {
				onReceiveElse(msg);
			}
		}
		
		private void onReceiveElse(Object msg) {
			if(msg instanceof GetColumns) {
				sender = getSender();
				sendColumns();
			} else {
				unhandled(msg);
			}
		}
		
		private void sendColumns() {
			if(sender != null && columns != null) {
				sender.tell(columns, getSelf());
				
				sender = null;
				columns = null;
			}
		}
		
		private Procedure<Object> sendingRecords(Iterable<Records> records) {
			final Iterator<Records> itr = records.iterator();			
			
			return new Procedure<Object>() {
				
				{
					sendResponse();
				}
				
				void sendResponse() {
					if(itr.hasNext()) {
						getSender().tell(itr.next(), getSelf());
					} else {
						getSender().tell(new End(), getSelf());
						getContext().unbecome();
					}
				}
				
				@Override
				public void apply(Object msg) throws Exception {
					log.debug("received: " + msg);
					
					if(msg instanceof NextItem) {
						sendResponse();
					} else {
						onReceiveElse(msg);
					}
				}
			};
		}

		private Procedure<Object> waitingForStart() {
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					log.debug("received: " + msg);
					
					if(msg instanceof Ack) {
						List<Records> records = new ArrayList<>();
						for(int i = 0; i < 2; i++) {
							
							List<Record> currentRecords = new ArrayList<>();
							for(int j = 0; j < 5; j++) {
								int value = i * 5 + j;
								
								currentRecords.add(
									new Record(
										Arrays.<Object>asList(
												"value: " + j, 
												value)));
							}
							
							records.add(new Records(currentRecords));
						}
						
						getContext().become(sendingRecords(records));
					} else {
						onReceiveElse(msg);
					}
				}
				
			};
		}
		
	}
	
	static class HarvesterMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		final ActorRef dataSource;
		
		HarvesterMock(ActorRef dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof GetDataSource) {
				getSender().tell(dataSource, getSelf());
			} else {
				unhandled(msg);
			}
		}		
	}
	
	static class GetFinishedState {
		
	}
	
	static class DatabaseAdapter extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		final ActorRef database;
		
		ActorRef sender = null;
		JobState finishedState = null;
		
		DatabaseAdapter(ActorRef database) {
			this.database = database;
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			log.debug("received: " + msg);
			
			if(msg instanceof GetFinishedState) {
				sender = getSender();
				sendFinishedState();
			} else {			
				if(msg instanceof UpdateJobState) {
					UpdateJobState ujs = (UpdateJobState)msg;
					
					JobState currentState = ujs.getState();
					if(currentState.isFinished()) {					
						finishedState = currentState;
						sendFinishedState();					
					}
				}
				
				database.tell(msg, getSender());
			}
		}
		
		void sendFinishedState() {
			if(sender != null && finishedState != null) {
				sender.tell(finishedState, getSelf());
				
				sender = null;
				finishedState = null;
			}
		}
		
	}
	
	ActorRef loader;
	ActorRef dataSourceMock;
	ActorRef databaseAdapter;
	ActorRef geometryDatabaseMock;
	
	@Before
	public void actors() {
		geometryDatabaseMock = actorOf(Props.create(GeometryDatabaseMock.class), "geometryDatabaseMock");
		dataSourceMock = actorOf(Props.create(DataSourceMock.class), "dataSourceMock");
		ActorRef harvesterMock = actorOf(Props.create(HarvesterMock.class, dataSourceMock), "harvesterMock");		
		databaseAdapter = actorOf(Props.create(DatabaseAdapter.class, database), "databaseAdapter");
		
		loader = actorOf(Loader.props(geometryDatabaseMock, databaseAdapter, harvesterMock), "loader");
	}

	@Test
	public void testExecuteImportJob() throws Exception {
		insertDataset();
		sync.ask(jobManager, new CreateImportJob("testDataset"));
		
		TypedIterable<?> iterable = sync.ask(jobManager, new GetImportJobs(), TypedIterable.class);
		assertTrue(iterable.contains(ImportJobInfo.class));
		for(ImportJobInfo job : iterable.cast(ImportJobInfo.class)) {
			sync.ask(loader, job, Ack.class);
		}
		
		assertEquals(
				JobState.SUCCEEDED, 				
				sync.ask(databaseAdapter, new GetFinishedState(), JobState.class));
		
		List<?> columns = sync.ask(dataSourceMock, new GetColumns(), List.class);
		Iterator<?> columnItr = columns.iterator();
		assertTrue(columnItr.hasNext());
		assertEquals("col0", columnItr.next());
		assertTrue(columnItr.hasNext());
		assertEquals("col1", columnItr.next());
		assertFalse(columnItr.hasNext());
		
		int insertCount = sync.ask(
				geometryDatabaseMock, 
				new GetInsertCount(), 
				Integer.class);
		
		assertEquals(10, insertCount);
	}
	
	@Test
	public void testAddColumnsChangedNotification() throws Exception {
		insertDataSource();		
		
		VectorDataset testDataset = createTestDataset();
		Table testTable = testDataset.getTable();
		List<Column> testColumns = testTable.getColumns();
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", testDataset), Registered.class);
		
		sync.ask(database, new CreateDataset(
				"testDataset", 
				"My Test Dataset", 
				testDataset.getId(), 
				testColumns, 
				"{ \"expression\": null }"));
				
		sync.ask(jobManager, new CreateImportJob("testDataset"));
		executeJobs(new GetImportJobs());
		
		Table updatedTable = new Table(
				testTable.getName(), 
				Arrays.asList(testColumns.get(0)));		
		VectorDataset updatedDataset = new VectorDataset(
				testDataset.getId(),
				testDataset.getCategoryId(),				
				testDataset.getRevisionDate(),
				Collections.<Log>emptySet(),
				updatedTable);
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", updatedDataset), Updated.class);
		sync.ask(jobManager, new CreateImportJob("testDataset"));
		
		TypedIterable<?> iterable = sync.ask(jobManager, new GetImportJobs(), TypedIterable.class);
		assertTrue(iterable.contains(ImportJobInfo.class));
		for(ImportJobInfo jobInfo : iterable.cast(ImportJobInfo.class)) {
			assertFalse(jobInfo.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED));
			sync.ask(loader, jobInfo, Ack.class);
		}
		
		iterable = sync.ask(jobManager, new GetImportJobs(), TypedIterable.class);
		assertTrue(iterable.contains(ImportJobInfo.class));
		
		Set<Integer> pendingJobs = new HashSet<>();
		for(ImportJobInfo jobInfo : iterable.cast(ImportJobInfo.class)) {
			pendingJobs.add(jobInfo.getId());
			
			assertTrue(jobInfo.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED));
			
			for(Notification notification : jobInfo.getNotifications()) {
				if(notification.getType() == ImportNotificationType.SOURCE_COLUMNS_CHANGED) {
					assertNull(notification.getResult());
				}
			}
		}	
		
		for(ImportJobInfo jobInfo : iterable.cast(ImportJobInfo.class)) {
			if(jobInfo.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED)) {
				sync.ask(database, new AddNotificationResult(
						jobInfo, 
						ImportNotificationType.SOURCE_COLUMNS_CHANGED, 
						ConfirmNotificationResult.OK));
			}
		}
				
		iterable = sync.ask(jobManager, new GetImportJobs(), TypedIterable.class);
		assertTrue(iterable.contains(ImportJobInfo.class));
		for(ImportJobInfo jobInfo : iterable.cast(ImportJobInfo.class)) {
			assertTrue(jobInfo.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED));
			
			for(Notification notification : jobInfo.getNotifications()) {
				if(notification.getType() == ImportNotificationType.SOURCE_COLUMNS_CHANGED) {
					assertEquals(ConfirmNotificationResult.OK, notification.getResult());
				}
			}
			
			sync.ask(loader, jobInfo, Ack.class);
		}
		
		// the loader is still able to import, but informs us 
		// about a missing column
		assertEquals(
				JobState.SUCCEEDED,
				sync.ask(databaseAdapter, new GetFinishedState(), JobState.class));
		
		InfoList<?> infoList = sync.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class);
		assertEquals(1, infoList.getCount().intValue());
		
		StoredJobLog jobLog = (StoredJobLog)infoList.getList().get(0);
		assertEquals(LogLevel.WARNING, jobLog.getLevel());
		assertEquals(ImportLogType.MISSING_COLUMNS, jobLog.getType());
		
		MessageProperties logContent = jobLog.getContent();
		assertNotNull(logContent);
		assertTrue(logContent instanceof MissingColumnsLog);
		
		MissingColumnsLog missingColumnsLog = (MissingColumnsLog)logContent;
		assertEquals("testDataset", missingColumnsLog.getIdentification());
		assertEquals("My Test Dataset", missingColumnsLog.getTitle());
		assertEquals(EntityType.DATASET, missingColumnsLog.getEntityType());
		
		Set<Column> missingColumns = missingColumnsLog.getColumns();
		assertNotNull(missingColumns);
		assertTrue(missingColumns.contains(testColumns.get(1)));
		
		// loader shouldn't request those missing columns
		List<?> columns = sync.ask(dataSourceMock, new GetColumns(), List.class);
		Iterator<?> columnItr = columns.iterator();
		assertTrue(columnItr.hasNext());
		assertEquals("col0", columnItr.next());
		assertFalse(columnItr.hasNext());
		
		// update dataset to be in line with the latest source dataset
		sync.ask(database, new UpdateDataset(
				"testDataset", 
				"My Test Dataset", 
				"testSourceDataset", 
				Arrays.asList(testColumns.get(0)),
				"{ \"expression\": null }"));
		
		sync.ask(jobManager, new CreateImportJob("testDataset"));
		
		iterable = sync.ask(jobManager, new GetImportJobs(), TypedIterable.class);
		assertTrue(iterable.contains(ImportJobInfo.class));
		
		Iterator<ImportJobInfo> itr = iterable.cast(ImportJobInfo.class).iterator();
		assertTrue(itr.hasNext());		
		sync.ask(loader, itr.next(), Ack.class);
		assertFalse(itr.hasNext());
		
		assertEquals(
				JobState.SUCCEEDED,
				sync.ask(databaseAdapter, new GetFinishedState(), JobState.class));		
		
		int count = sync.ask(geometryDatabaseMock, new GetInsertCount(), Integer.class);
		assertEquals(10, count);
		
		// verify that the loader doens't inform us about missing 
		// columns anymore (because we updated the dataset)
		infoList = sync.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class);
		assertEquals(1, infoList.getCount().intValue());
	}
	
	@Test
	public void testMissingColumns() throws Exception {
		insertDataset();
		
		int datasetId = query().from(dataset)
			.where(dataset.identification.eq("testDataset"))
			.singleResult(dataset.id);
		
		insert(datasetColumn)
			.set(datasetColumn.datasetId, datasetId)
			.set(datasetColumn.name, "col3")
			.set(datasetColumn.dataType, Type.TEXT.name())
			.set(datasetColumn.index, 3)	
			.execute();
		
		insert(datasetColumn)
			.set(datasetColumn.datasetId, datasetId)
			.set(datasetColumn.name, "col4")
			.set(datasetColumn.dataType, Type.GEOMETRY.name())
			.set(datasetColumn.index, 4)	
			.execute();
			
		Filter filter = new Filter(
				new OperatorExpression(
						OperatorType.EQUALS, 
						Arrays.asList(
								new ColumnReferenceExpression(new Column("col3", Type.TEXT)),
								new ValueExpression(Type.TEXT, "filterValue"))));
		
		ObjectMapper mapper = new ObjectMapper();
		
		update(dataset)
			.set(dataset.filterConditions, mapper.writeValueAsString(filter)) 
			.execute();
		
		sync.ask(jobManager, new CreateImportJob("testDataset"));
		
		TypedIterable<?> importJobIterable = sync.ask(jobManager, new GetImportJobs(), TypedIterable.class);
		assertTrue(importJobIterable.contains(ImportJobInfo.class));
		
		Iterator<ImportJobInfo> importJobItr = importJobIterable.cast(ImportJobInfo.class).iterator();
		assertTrue(importJobItr.hasNext());
		sync.ask(loader, importJobItr.next(), Ack.class);
		assertFalse(importJobItr.hasNext());
		
		assertEquals(
				JobState.FAILED,
				
				sync.ask(databaseAdapter, new GetFinishedState(), JobState.class));
		
		InfoList<?> infoList = sync.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class);
		assertEquals(2, infoList.getCount().intValue());
		
		update(dataset)
			.set(dataset.filterConditions, mapper.writeValueAsString(new Filter(null))) 
			.execute();
		
		sync.ask(jobManager, new CreateImportJob("testDataset"));
		
		importJobIterable = sync.ask(jobManager, new GetImportJobs(), TypedIterable.class);
		assertTrue(importJobIterable.contains(ImportJobInfo.class));
		
		importJobItr = importJobIterable.cast(ImportJobInfo.class).iterator();
		assertTrue(importJobItr.hasNext());
		sync.ask(loader, importJobItr.next(), Ack.class);
		assertFalse(importJobItr.hasNext());
		
		assertEquals(
				JobState.SUCCEEDED,
				
				sync.ask(databaseAdapter, new GetFinishedState(), JobState.class));
		
		infoList = sync.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class);
		assertEquals(3, infoList.getCount().intValue());
	}
}
