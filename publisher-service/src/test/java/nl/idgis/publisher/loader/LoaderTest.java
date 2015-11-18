package nl.idgis.publisher.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static nl.idgis.publisher.database.QJobState.jobState;
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

import nl.idgis.publisher.AbstractServiceTest;

import nl.idgis.publisher.database.messages.AddNotificationResult;
import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.GetJobLog;
import nl.idgis.publisher.database.messages.InfoList;
import nl.idgis.publisher.database.messages.InsertRecords;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.StoredJobLog;
import nl.idgis.publisher.database.messages.TransactionCreated;

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

import nl.idgis.publisher.job.JobExecutorFacade;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.loader.messages.GetRequestedColumns;
import nl.idgis.publisher.loader.messages.SetRecordsResponse;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
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
					
			if(msg instanceof InsertRecords) {
				insertCount += ((InsertRecords)msg).getRecords().size();
			} else if(msg instanceof Commit) {
				getContext().parent().tell(new SetInsertCount(insertCount), getSelf());
				getContext().stop(getSelf());
			}
		
			getSender().tell(new Ack(), getSelf());
		}
		
	}
	
	static class DatabaseMock extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		final ActorRef database;
		
		ActorRef sender;
	
		Integer insertCount;
		
		public DatabaseMock(ActorRef database)  {
			this.database = database;
		}
		
		static Props props(ActorRef database) {
			return Props.create(DatabaseMock.class, database);
		}

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
				database.forward(msg, getContext());
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
	
	static class RasterFolderMock extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			unhandled(msg);
		}

		static Props props() {
			return Props.create(RasterFolderMock.class);
		}
		
	}
	
	ActorRef loader, dataSourceMock, databaseMock, rasterFolderMock;
	
	@Before
	public void actors() throws Exception {
		databaseMock = actorOf(DatabaseMock.props(database), "databaseMock");
		dataSourceMock = actorOf(DataSourceMock.props(), "dataSourceMock");
		rasterFolderMock = actorOf(RasterFolderMock.props(), "rasterFolderMock");
		ActorRef harvesterMock = actorOf(HarvesterMock.props(dataSourceMock), "harvesterMock");		
		
		loader = actorOf(JobExecutorFacade.props(jobManager, actorOf(Loader.props(databaseMock, rasterFolderMock, harvesterMock, datasetManager), "loader")), "loaderFacade");
		
		List<Records> content = new ArrayList<>();
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
			
			content.add(new Records(currentRecords));
		}
		
		f.ask(dataSourceMock, new SetRecordsResponse(content), Ack.class).get();
	}

	@Test
	public void testExecuteImportJob() throws Exception {
		insertDataset();
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		
		TypedIterable<?> iterable = f.ask(jobManager, new GetImportJobs(), TypedIterable.class).get();
		assertTrue(iterable.contains(ImportJobInfo.class));
		for(ImportJobInfo job : iterable.cast(ImportJobInfo.class)) {
			f.ask(loader, job, Ack.class).get();
			assertFinishedJobState(JobState.SUCCEEDED, job);
		}
		
		List<?> columns = f.ask(dataSourceMock, new GetRequestedColumns(), List.class).get();
		Iterator<?> columnItr = columns.iterator();
		assertTrue(columnItr.hasNext());
		assertEquals("col0", columnItr.next());
		assertTrue(columnItr.hasNext());
		assertEquals("col1", columnItr.next());
		assertFalse(columnItr.hasNext());
		
		int insertCount = f.ask(
				databaseMock, 
				new GetInsertCount(), 
				Integer.class).get();
		
		assertEquals(10, insertCount);
	}
	
	@Test
	public void testAddColumnsChangedNotification() throws Exception {
		insertDataSource();		
		
		VectorDataset testDataset = createVectorDataset();
		Table testTable = testDataset.getTable();
		List<Column> testColumns = testTable.getColumns();
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", testDataset), Registered.class).get();
		
		createDataset(
				"testDataset", 
				"My Test Dataset", 
				testDataset.getId(), 
				testColumns, 
				"{ \"expression\": null }");
				
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		executeJobs(new GetImportJobs());
		
		Table updatedTable = new Table(Arrays.asList(testColumns.get(0)));		
		VectorDataset updatedDataset = new VectorDataset(
				testDataset.getId(),
				testDataset.getName(),
				testDataset.getAlternateTitle(),
				testDataset.getCategoryId(),				
				testDataset.getRevisionDate(),
				Collections.<Log>emptySet(),
				false,
				updatedTable);
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", updatedDataset), Updated.class).get();
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		
		TypedIterable<?> iterable = f.ask(jobManager, new GetImportJobs(), TypedIterable.class).get();
		assertTrue(iterable.contains(ImportJobInfo.class));
		for(ImportJobInfo jobInfo : iterable.cast(ImportJobInfo.class)) {
			assertFalse(jobInfo.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED));
			f.ask(loader, jobInfo, Ack.class).get();
		}
		
		iterable = f.ask(jobManager, new GetImportJobs(), TypedIterable.class).get();
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
				f.ask(database, new AddNotificationResult(
						jobInfo, 
						ImportNotificationType.SOURCE_COLUMNS_CHANGED, 
						ConfirmNotificationResult.OK)).get();
			}
		}
				
		iterable = f.ask(jobManager, new GetImportJobs(), TypedIterable.class).get();
		assertTrue(iterable.contains(ImportJobInfo.class));
		for(ImportJobInfo jobInfo : iterable.cast(ImportJobInfo.class)) {
			assertTrue(jobInfo.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED));
			
			for(Notification notification : jobInfo.getNotifications()) {
				if(notification.getType() == ImportNotificationType.SOURCE_COLUMNS_CHANGED) {
					assertEquals(ConfirmNotificationResult.OK, notification.getResult());
				}
			}
			
			f.ask(loader, jobInfo, Ack.class).get();
			assertFinishedJobState(JobState.SUCCEEDED, jobInfo);			
		}
		
		// the loader is still able to import, but informs us 
		// about a missing column
		InfoList<?> infoList = f.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class).get();
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
		List<?> columns = f.ask(dataSourceMock, new GetRequestedColumns(), List.class).get();
		Iterator<?> columnItr = columns.iterator();
		assertTrue(columnItr.hasNext());
		assertEquals("col0", columnItr.next());
		assertFalse(columnItr.hasNext());
		
		// update dataset to be in line with the latest source dataset
		updateDataset(
				"testDataset", 
				"My Test Dataset", 
				"testVectorDataset", 
				Arrays.asList(testColumns.get(0)),
				"{ \"expression\": null }");
		
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		
		iterable = f.ask(jobManager, new GetImportJobs(), TypedIterable.class).get();
		assertTrue(iterable.contains(ImportJobInfo.class));
		
		Iterator<ImportJobInfo> itr = iterable.cast(ImportJobInfo.class).iterator();
		assertTrue(itr.hasNext());
		ImportJobInfo job = itr.next();
		f.ask(loader, job, Ack.class).get();
		assertFalse(itr.hasNext());
		
		assertFinishedJobState(JobState.SUCCEEDED, job);
		
		int count = f.ask(databaseMock, new GetInsertCount(), Integer.class).get();
		assertEquals(10, count);
		
		// verify that the loader doens't inform us about missing 
		// columns anymore (because we updated the dataset)
		infoList = f.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class).get();
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
		
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		
		TypedIterable<?> importJobIterable = f.ask(jobManager, new GetImportJobs(), TypedIterable.class).get();
		assertTrue(importJobIterable.contains(ImportJobInfo.class));
		
		Iterator<ImportJobInfo> importJobItr = importJobIterable.cast(ImportJobInfo.class).iterator();
		assertTrue(importJobItr.hasNext());
		ImportJobInfo job = importJobItr.next(); 
		f.ask(loader, job, Ack.class).get();
		assertFalse(importJobItr.hasNext());
		
		assertFinishedJobState(JobState.FAILED, job);
		
		InfoList<?> infoList = f.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class).get();
		assertEquals(2, infoList.getCount().intValue());
		
		update(dataset)
			.set(dataset.filterConditions, mapper.writeValueAsString(new Filter(null))) 
			.execute();
		
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		
		importJobIterable = f.ask(jobManager, new GetImportJobs(), TypedIterable.class).get();
		assertTrue(importJobIterable.contains(ImportJobInfo.class));
		
		importJobItr = importJobIterable.cast(ImportJobInfo.class).iterator();
		assertTrue(importJobItr.hasNext());
		job = importJobItr.next();
		f.ask(loader, job, Ack.class).get();
		assertFalse(importJobItr.hasNext());
		
		assertFinishedJobState(JobState.SUCCEEDED, job);
		
		infoList = f.ask(database, new GetJobLog(LogLevel.DEBUG), InfoList.class).get();
		assertEquals(3, infoList.getCount().intValue());
	}

	private void assertFinishedJobState(JobState expected, JobInfo job) throws Exception {
		JobState encountered;
		while(!(encountered = JobState.valueOf(
			query().from(jobState)
				.where(jobState.jobId.eq(job.getId()))
				.orderBy(jobState.id.desc())
				.singleResult(jobState.state))).isFinished()) {
			
			Thread.sleep(100);
		}
		
		assertEquals(encountered, expected);
	}
}
