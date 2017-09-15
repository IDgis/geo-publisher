package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetMetadataAttachment.sourceDatasetMetadataAttachment;
import static nl.idgis.publisher.database.QSourceDatasetMetadataAttachmentError.sourceDatasetMetadataAttachmentError;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionLog.sourceDatasetVersionLog;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.QTuple;
import com.mysema.query.types.expr.DateTimeExpression;

import nl.idgis.publisher.dataset.messages.AlreadyRegistered;
import nl.idgis.publisher.dataset.messages.Cleanup;
import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;
import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.DatabaseLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.service.UnavailableDataset;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.AbstractServiceTest;

public class DatasetManagerTest extends AbstractServiceTest {
	
	private Server jettyServer;

	@Before
	public void dataSource() {
		insertDataSource();
	}
	
	private int httpStatus = 200;
	
	// http server is hosting (fake) metadata attachments
	@Before
	public void startHttpServer() throws Exception {
		jettyServer = new Server(7000);
		jettyServer.setHandler(new AbstractHandler() {

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				response.setStatus(httpStatus);
				response.setContentType("text/plain");
				
				try(PrintWriter writer = response.getWriter()) {
					writer.println("Hello, world!");
				}
			}
			
		});
		jettyServer.start();
	}
	
	@After
	public void stopHttpServer() throws Exception {
		jettyServer.stop();
	}
	
	@Test
	public void testRegisterNewUnavailableDataset() throws Exception {
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", createUnavailableDataset()), Registered.class).get();
		
		assertTrue(query().from(sourceDatasetVersion).exists());
		assertTrue(query().from(sourceDatasetVersionLog).exists());
	}
	
	@Test
	public void testRegisterUpdateUnavailableDataset() throws Exception {
		UnavailableDataset dataset = createUnavailableDataset();
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Registered.class).get();
		
		assertTrue(query().from(sourceDatasetVersion).exists());
		
		Set<Log> logs = new HashSet<>(dataset.getLogs());
		dataset = new UnavailableDataset(
			dataset.getId(), 
			dataset.getName(),
			dataset.getAlternateTitle(),
			dataset.getCategoryId(), 
			dataset.getRevisionDate(),
			logs,
			dataset.isConfidential(),
			dataset.isMetadataConfidential(),
			dataset.isWmsOnly(),
			dataset.getMetadata().orElse(null),
			dataset.getPhysicalName(),
			dataset.getRefreshFrequency());
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), AlreadyRegistered.class).get();
		
		logs = new HashSet<>();
		logs.add(Log.create(LogLevel.ERROR, DatasetLogType.TABLE_NOT_FOUND, new DatabaseLog("my_table")));
		dataset = new UnavailableDataset(
			dataset.getId(),
			dataset.getName(),
			dataset.getAlternateTitle(),
			dataset.getCategoryId(), 
			dataset.getRevisionDate(), 
			logs,
			dataset.isConfidential(),
			dataset.isMetadataConfidential(),
			dataset.isWmsOnly(),
			dataset.getMetadata().orElse(null),
			dataset.getPhysicalName(),
			dataset.getRefreshFrequency());
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Updated.class).get();
	}

	@Test
	public void testRegisterNewVectorDataset() throws Exception {
		assertFalse(
			query().from(sourceDatasetMetadata)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadata.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.exists());
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", createVectorDataset()), Registered.class).get();		
		
		assertTrue(
			query().from(category)
			.where(category.identification.eq("testCategory"))
			.exists());
		
		assertTrue(
			query().from(sourceDataset)
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.exists());
		
		assertEquals(1,
			query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.singleResult(sourceDatasetVersion.id.count()).intValue());
		
		assertTrue(
			query().from(sourceDatasetMetadata)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadata.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.exists());
	}
	
	@Test
	public void testRegisterUpdateVectorDataset() throws Exception {
		// fill database with other source datasets
		for(int i = 0; i < 100; i++) {
			f.ask(datasetManager, new RegisterSourceDataset("testDataSource", createVectorDataset("otherSourceDataset" + i)), Registered.class).get();
		}
		
		VectorDataset dataset = createVectorDataset();		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Registered.class).get();
	
		assertTrue(
			query().from(sourceDatasetMetadata)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadata.sourceDatasetId))
			.exists());
		
		// expect two attachment (graphicOverview and supplementalInformation)
		assertEquals(
			2,
			query().from(sourceDatasetMetadataAttachment)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadataAttachment.sourceDatasetId))
			.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
			.count());
		
		// remove metadata attachments
		delete(sourceDatasetMetadataAttachment)
			.where(new SQLSubQuery()
				.from(sourceDataset)
				.where(sourceDataset.id.eq(sourceDatasetMetadataAttachment.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.exists())
			.execute();
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), AlreadyRegistered.class).get();
		
		// dataset manager should have restored missing attachment
		assertTrue(
			query().from(sourceDatasetMetadataAttachment)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadataAttachment.sourceDatasetId))
			.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
			.exists());
		
		Thread.sleep(1000); // createTestDataset() uses current time as revision date
		
		// destroy metadata
		update(sourceDatasetMetadata)
			.set(sourceDatasetMetadata.document, "Hello, world!".getBytes("utf-8"))
			.execute();
		
		// remove metadata attachments
		delete(sourceDatasetMetadataAttachment)
			.where(new SQLSubQuery()
				.from(sourceDataset)
				.where(sourceDataset.id.eq(sourceDatasetMetadataAttachment.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.exists())
			.execute();
		
		VectorDataset updatedDataset = createVectorDataset();
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", updatedDataset), Updated.class).get();		
		
		assertEquals(2,
				query().from(sourceDatasetVersion)
					.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
					.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
					.singleResult(sourceDatasetVersion.id.count()).intValue());
		
		Iterator<Timestamp> itr = query().from(sourceDatasetVersion)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
			.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
			.orderBy(sourceDatasetVersion.id.asc())
			.list(sourceDatasetVersion.revision).iterator();
		
		assertTrue(itr.hasNext());
		
		Timestamp t = itr.next();
		assertEquals(dataset.getRevisionDate().getTime(), t.getTime());
		assertNotNull(t);
		
		assertTrue(itr.hasNext());
		t = itr.next();
		assertEquals(updatedDataset.getRevisionDate().getTime(), t.getTime());
		assertNotNull(t);
		
		assertFalse(itr.hasNext());
		
		Tuple tuple =
			query().from(sourceDatasetMetadata)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadata.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.singleResult(new QTuple(sourceDatasetMetadata.document));
		
		// singleResult(sourceDatasetMetadata.document) results in
		// a ClassCastException, unclear why.
		
		byte[] metadataDocument = tuple.get(sourceDatasetMetadata.document);		
		assertNotNull(metadataDocument);
		
		// updating the dataset should have restored the metadata
		assertNotEquals("Hello, world!", new String(metadataDocument, "utf-8"));
		assertTrue(
			query().from(sourceDatasetMetadataAttachment)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadataAttachment.sourceDatasetId))
			.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
			.exists());
		assertFalse(
			query().from(sourceDatasetMetadataAttachmentError)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadataAttachmentError.sourceDatasetId))
			.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
			.exists());
		
		httpStatus = 404; // disable mock http service
		delete(sourceDatasetMetadataAttachment) // remove downloaded attachments
			.where(new SQLSubQuery()
				.from(sourceDataset)
				.where(sourceDataset.id.eq(sourceDatasetMetadataAttachment.sourceDatasetId))
				.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
				.exists())
			.execute();
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", updatedDataset), AlreadyRegistered.class).get();
		
		// couldn't redownload attachment
		assertFalse(
			query().from(sourceDatasetMetadataAttachment)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadataAttachment.sourceDatasetId))
			.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
			.exists());
		
		assertTrue(
			query().from(sourceDatasetMetadataAttachmentError)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetMetadataAttachmentError.sourceDatasetId))
			.where(sourceDataset.externalIdentification.eq("testVectorDataset"))
			.exists());
	}
	
	@Test
	public void testRegisterUnavailableDatasetNoCategory() throws Exception {
		UnavailableDataset dataset = createUnavailableDataset();
		dataset = new UnavailableDataset(
				dataset.getId(), 
				dataset.getName(),
				dataset.getAlternateTitle(),
				null, //categoryId removed 
				dataset.getRevisionDate(), 
				dataset.getLogs(),
				dataset.isConfidential(),
				dataset.isMetadataConfidential(),
				dataset.isWmsOnly(),
				dataset.getMetadata().orElse(null),
				dataset.getPhysicalName(),
				dataset.getRefreshFrequency());
		
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Registered.class).get();
		
		// verifies that the dataset manager is able to retrieve datasets without a categoryId 
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), AlreadyRegistered.class).get();
	}

	private void deleteSourceDataset (final String id) {
		update(sourceDataset)
			.set(sourceDataset.deleteTime, DateTimeExpression.currentTimestamp(Timestamp.class))
			.where(sourceDataset.externalIdentification.eq(id))
			.execute();
	}
	
	@Test
	public void testCleanup () throws Exception {
		final List<Column> columns = Arrays.asList(
				new Column("col0", Type.TEXT, null /*alias*/),
				new Column("col1", Type.NUMERIC, null));
		final Table table = new Table(columns);
		final Timestamp revision = new Timestamp(new Date().getTime());
		final VectorDataset[] datasets = {
			new VectorDataset ("table1", "My Test Table", "alternate title", "category1", revision, Collections.<Log>emptySet(), false, false, false, null, table, null, null),
			new VectorDataset ("table2", "My Test Table 2", "alternate title", "category2", revision, Collections.<Log>emptySet(), true, false, false, null, table, null, null),
			new VectorDataset ("table3", "My Test Table 3", "alternate title", "category1", revision, Collections.<Log>emptySet(), false, false, false, null, table, null, null),
			new VectorDataset ("table4", "My Test Table 4", "alternate title", "category2", revision, Collections.<Log>emptySet(), true, false, false, null, table, null, null)
		};
		
		for (final VectorDataset ds: datasets) {
			f.ask(datasetManager, new RegisterSourceDataset("testDataSource", ds), Registered.class).get();		
		}
		
		assertEquals (4, query ().from (sourceDataset).count ());
		// After inserting 4 source datasets with two different categories, there should be two categories:
		assertEquals (2, query ().from (category).count ());		

		deleteSourceDataset ("table1");
		f.ask (datasetManager, new Cleanup ()).get ();
		
		// Everything stays the same since implementation of harvest notifications, because it only will 
		// be deleted when there are no open notification:
		assertEquals (4, query ().from (sourceDataset).count ());
		assertEquals (2, query ().from (category).count ());
		
		deleteSourceDataset ("table3");
		f.ask (datasetManager, new Cleanup ()).get ();
		
		assertEquals (4, query ().from (sourceDataset).count ());
		assertEquals (2, query ().from (category).count ());
		
		deleteSourceDataset ("table2");
		f.ask (datasetManager, new Cleanup ()).get ();
		
		assertEquals (4, query ().from (sourceDataset).count ());
		assertEquals (2, query ().from (category).count ());
		
		deleteSourceDataset ("table4");
		f.ask (datasetManager, new Cleanup ()).get ();
		
		assertEquals (4, query ().from (sourceDataset).count ());
		assertEquals (2, query ().from (category).count ());
	}
}
