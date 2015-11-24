package nl.idgis.publisher.loader;

import static nl.idgis.publisher.database.QDatasetView.datasetView;
import static nl.idgis.publisher.database.QDatasetCopy.datasetCopy;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetColumnDiff.sourceDatasetColumnDiff;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.mysema.query.sql.SQLSubQuery;

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
import nl.idgis.publisher.domain.web.tree.Service;

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
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.PublishService;
import nl.idgis.publisher.utils.TypedList;

public class MissingColumnTest extends AbstractServiceTest {
	
	ActorRef dataSource, harvester, loader;
	
	@Before
	public void setUp() {
		dataSource = actorOf(DataSourceMock.props(), "dataSource");
		harvester = actorOf(HarvesterMock.props(dataSource), "harvester");		
		loader = actorOf(Loader.props(database, null, harvester, datasetManager), "loader");
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
		
		assertDatasetRel(datasetId, "staging_data", 1, "col0", "col1");
		assertRelNotExists("data", datasetId);
				
		// create layer and service
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "testLayer")
			.set(genericLayer.name, "testLayerName")
			.executeWithKey(genericLayer.id);
		
		int serviceLayerId = insert(genericLayer)
			.set(genericLayer.identification, "testService")
			.set(genericLayer.name, "testServiceName")
			.executeWithKey(genericLayer.id);
		
		insert(service)
			.set(service.genericLayerId, serviceLayerId)
			.execute();
		
		insert(leafLayer)
			.columns(
				leafLayer.genericLayerId,
				leafLayer.datasetId)
			.select(
				new SQLSubQuery().from(dataset)
				.where(dataset.identification.eq(datasetId))
				.list(layerId, dataset.id))
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, serviceLayerId)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		f.ask(serviceManager, new GetService("testService"), Service.class).get();
		
		// publish service
		insert(environment)
			.set(environment.identification, "testEnvironment")
			.set(environment.confidential, false)
			.execute();
		
		f.ask(serviceManager, new PublishService("testService", Collections.singleton("testEnvironment")), Ack.class).get();
		
		assertDatasetRel(datasetId, "data", 1, "col0", "col1");
		assertDatasetView(datasetId, "col0", "col1");
		
		// drop second column ('col1')
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
		
		// expected: no change yet
		assertDatasetRel(datasetId, "staging_data", 1, "col0", "col1");
		assertDatasetRel(datasetId, "data", 1, "col0", "col1");
		assertDatasetView(datasetId, "col0", "col1");
		
		// accept structure change
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
		
		assertDatasetRel(datasetId, "staging_data", 1, "col0");
		assertDatasetRel(datasetId, "data", 1, "col0", "col1");
		assertDatasetCopy(datasetId, "col0", "col1");
		
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
		
		assertDatasetRel(datasetId, "staging_data", 1, "col0");
		assertDatasetRel(datasetId, "data", 1, "col0", "col1");
		assertDatasetCopy(datasetId, "col0", "col1");
		
		f.ask(
			dataSource, 
			new SetRecordsResponse(
				Collections.singletonList(
					new Records(
						Collections.singletonList(
							new Record(
								Arrays.asList("Hello, world!", 42)))))), 
			Ack.class).get();
		
		// create additional datasets
		for(int i = 0; i < 10; i++) {
			final String anotherSourceDatasetId = "anotherSourceDataset" + i;
			
			VectorDataset anotherDataset = new VectorDataset(
				anotherSourceDatasetId, 
				"My Test Table", 
				"alternate title", 
				"testCategory", 
				new Timestamp(new Date().getTime()), //revision date
				Collections.<Log>emptySet(), 
				false, // confidential
				new Table(columns));
			
			f.ask(datasetManager, new RegisterSourceDataset("testDataSource", anotherDataset), Registered.class).get();
			
			String additionalDatasetId = "additionalDataset" + i;
			
			createDataset(
				additionalDatasetId, 
				"My Test Dataset", 
				anotherSourceDatasetId,
				columns, 
				"{ \"expression\": null }");
			
			f.ask(recorder, new Clear(), Cleared.class).get();
			
			f.ask(jobManager, new CreateImportJob(additionalDatasetId)).get();
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
		
		// republish service		
		f.ask(serviceManager, new PublishService("testService", Collections.singleton("testEnvironment")), Ack.class).get();
		
		assertDatasetRel(datasetId, "staging_data", 1, "col0");
		assertDatasetRel(datasetId, "data", 1, "col0");
		assertDatasetView(datasetId, "col0");
	}

	private void assertDatasetView(String datasetId, String... columnNames) {
		assertEquals(
			Arrays.asList(columnNames),
			query().from(datasetView)
				.join(dataset).on(dataset.id.eq(datasetView.datasetId))
				.where(dataset.identification.eq(datasetId))
				.list(datasetView.name));
		assertFalse(
			query().from(datasetCopy)
				.join(dataset).on(dataset.id.eq(datasetCopy.datasetId))
				.where(dataset.identification.eq(datasetId))
				.exists());
	}
	
	private void assertDatasetCopy(String datasetId, String... columnNames) {
		assertEquals(
			Arrays.asList(columnNames),
			query().from(datasetCopy)
				.join(dataset).on(dataset.id.eq(datasetCopy.datasetId))
				.where(dataset.identification.eq(datasetId))
				.list(datasetCopy.name));
		assertFalse(
			query().from(datasetView)
				.join(dataset).on(dataset.id.eq(datasetView.datasetId))
				.where(dataset.identification.eq(datasetId))
				.exists());
	}

	private void assertRelNotExists(String schemaName, String tableName) {
		try(Statement stmt = statement();) {
			try(ResultSet rs = stmt.executeQuery("select * from \"" + schemaName + "\".\"" + tableName + "\"");) {
				while(rs.next()) { }
			}
			
			fail("relation " + schemaName + "." + tableName + " should not exists");
		} catch(SQLException e) { }
	}

	private void assertDatasetRel(final String datasetId, String schema, int count, String... columnNames) throws SQLException {
		Statement stmt = statement();
		
		ResultSet rs = stmt.executeQuery("select count(*) from " + schema + ".\"" + datasetId + "\"");
		assertTrue(rs.next());
		assertEquals(count, rs.getInt(1));
		assertFalse(rs.next());
		rs.close();
		
		rs = statement().executeQuery("select * from " + schema + ".\"" + datasetId + "\"");
		ResultSetMetaData md = rs.getMetaData();
		assertEquals(columnNames.length, md.getColumnCount());
		
		for(int i = 0; i < columnNames.length; i++) {		
			assertEquals(columnNames[i], md.getColumnName(i + 1));
		}
		
		rs.close();
		
		stmt.close();
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
