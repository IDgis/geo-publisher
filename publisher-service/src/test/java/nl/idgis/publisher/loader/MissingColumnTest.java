package nl.idgis.publisher.loader;

import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumnDiff.sourceDatasetColumnDiff;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.messages.AddNotificationResult;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;
import nl.idgis.publisher.job.context.JobContext;
import nl.idgis.publisher.job.context.messages.JobFinished;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.loader.messages.SetRecordsResponse;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.recorder.AnyAckRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.Create;
import nl.idgis.publisher.recorder.messages.Created;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.utils.TypedList;

public class MissingColumnTest extends AbstractServiceTest {
	
	ActorRef dataSource, harvester, loader;
	
	@Before
	public void setUp() {
		dataSource = actorOf(DataSourceMock.props(), "dataSource");
		harvester = actorOf(HarvesterMock.props(dataSource), "harvester");		
		loader = actorOf(Loader.props(database, null, harvester), "loader");
	}
	
	@Test
	public void testMissingColumn() throws Exception {		
		insertDataSource("testDataSource");
		
		// register source dataset
		List<Column> columns = Arrays.asList(
			new Column("col0", Type.TEXT),
			new Column("col1", Type.NUMERIC));
		
		final String sourceDatasetId = "testSourceDataset";
				
		VectorDataset testDataset = new VectorDataset(
			sourceDatasetId, 
			"My Test Table", 
			"alternate title", 
			"testCategory", 
			new Timestamp(new Date().getTime()), //revision date
			Collections.<Log>emptySet(), 
			false, // confidential
			new Table(columns));
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", testDataset), Registered.class).get();
		
		assertEquals(1, query().from(sourceDatasetVersion).count());
		
		// create dataset
		
		// using a UUID doesn't work in H2. The view in
		// the data schema is not working.
		final String datasetId = "testDataset";
		
		createDataset(
				datasetId, 
				"My Test Dataset", 
				sourceDatasetId,
				columns, 
				"{ \"expression\": null }");		
		
		// set data source mockup content
		f.ask(
			dataSource, 
			new SetRecordsResponse(
				Collections.singletonList(
					new Records(
						Collections.singletonList(
							new Record(
								Arrays.asList("Hello, world!", 42)))))), 
			Ack.class).get();
		
		ActorRef recorder = actorOf(AnyAckRecorder.props(new Ack()), "recorder");
				
		f.ask(jobManager, new CreateImportJob(datasetId)).get();		
		ImportJobInfo job = getNextImportJob();		
		loader.tell(
			job,
			f.ask(
				recorder, 
				new Create(JobContext.props(jobManager, recorder, job)), 
				Created.class).get()
					.getActorRef());
		
		f.ask(recorder, new Wait(2), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(Ack.class)
			.assertNext(JobFinished.class, msg -> assertEquals(JobState.SUCCEEDED, msg.getJobState()));
			

		ResultSet rs = statement().executeQuery("select count(*) from staging_data.\"" + datasetId + "\"");
		assertTrue(rs.next());
		assertEquals(1, rs.getInt(1));
		assertFalse(rs.next());
		
		rs = statement().executeQuery("select * from staging_data.\"" + datasetId + "\"");
		ResultSetMetaData md = rs.getMetaData();
		assertEquals(2, md.getColumnCount());
		assertEquals("col0", md.getColumnName(1));
		assertEquals("col1", md.getColumnName(2));
		
		rs = statement().executeQuery("select count(*) from data.\"" + datasetId + "\"");
		assertTrue(rs.next());
		assertEquals(1, rs.getInt(1));
		assertFalse(rs.next());
		
		rs = statement().executeQuery("select * from data.\"" + datasetId + "\"");
		md = rs.getMetaData();
		assertEquals(2, md.getColumnCount());
		assertEquals("col0", md.getColumnName(1));
		assertEquals("col1", md.getColumnName(2));
		
		// drop second column
		testDataset = new VectorDataset(
			testDataset.getId(), 
			testDataset.getName(),
			testDataset.getAlternateTitle(), 
			testDataset.getCategoryId(),
			testDataset.getRevisionDate(),
			testDataset.getLogs(), 
			testDataset.isConfidential(),
			new Table(Collections.singletonList(columns.get(0)))); 
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", testDataset), Updated.class).get();
		
		assertEquals(
			2, 
			query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq(sourceDatasetId))
				.count());
		
		assertEquals(3, query().from(sourceDatasetVersionColumn).count());
		
		assertTrue(query().from(sourceDatasetColumnDiff).exists());
		
		//Tuple t = query().from(sourceDatasetColumnDiff).singleResult(sourceDatasetColumnDiff.all());
		//assertNotNull(t);
		
		// set data source mockup content
		f.ask(
			dataSource, 
			new SetRecordsResponse(
				Collections.singletonList(
					new Records(
						Collections.singletonList(
							new Record(
								Arrays.asList("Hello, world!")))))), 
			Ack.class).get();
		
		//assertEquals("col1", query().from(datasetColumnDiff).singleResult(datasetColumnDiff.name));
		//assertTrue(query().from(datasetStatus).singleResult(datasetStatus.sourceDatasetColumnsChanged));
		
		// start another import, should result in a 
		// source columns changed notification
		f.ask(recorder, new Clear(), Cleared.class).get();
		
		f.ask(jobManager, new CreateImportJob(datasetId)).get();		
		job  = getNextImportJob();
		loader.tell(
			job,
			f.ask(
				recorder, 
				new Create(JobContext.props(jobManager, recorder, job)), 
				Created.class).get()
					.getActorRef());
		
		f.ask(recorder, new Wait(1), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(Ack.class);
		
		job = getNextImportJob();
		
		Iterator<Notification> notificationsItr = job.getNotifications().iterator();
		assertTrue(notificationsItr.hasNext());
		assertEquals(ImportNotificationType.SOURCE_COLUMNS_CHANGED, notificationsItr.next().getType());
		assertFalse(notificationsItr.hasNext());
		
		f.ask(database, new AddNotificationResult(
			job, 
			ImportNotificationType.SOURCE_COLUMNS_CHANGED, 
			ConfirmNotificationResult.OK)).get();
		
		// start another import, should succeed
		f.ask(recorder, new Clear(), Cleared.class).get();
		
		job = getNextImportJob();		
		loader.tell(
			job,
			f.ask(
				recorder, 
				new Create(JobContext.props(jobManager, recorder, job)), 
				Created.class).get()
					.getActorRef());
		
		f.ask(recorder, new Wait(2), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(Ack.class)
			.assertNext(JobFinished.class, msg -> assertEquals(JobState.SUCCEEDED, msg.getJobState()));
		
		// start (yet) another import, should succeed
		f.ask(recorder, new Clear(), Cleared.class).get();
		
		f.ask(jobManager, new CreateImportJob(datasetId)).get();
		job = getNextImportJob();		
		loader.tell(
			job,
			f.ask(
				recorder, 
				new Create(JobContext.props(jobManager, recorder, job)), 
				Created.class).get()
					.getActorRef());
		
		f.ask(recorder, new Wait(2), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(Ack.class)
			.assertNext(JobFinished.class, msg -> assertEquals(JobState.SUCCEEDED, msg.getJobState()));
	}	

	private ImportJobInfo getNextImportJob() throws InterruptedException, ExecutionException {
		Iterator<ImportJobInfo> itr = 
			((TypedList<?>)f.ask(jobManager, new GetImportJobs(), TypedList.class).get())
				.cast(ImportJobInfo.class).iterator();
		
		assertTrue(itr.hasNext());		
		ImportJobInfo job = itr.next();		
		assertFalse(itr.hasNext());
		
		return job;
	}
}
