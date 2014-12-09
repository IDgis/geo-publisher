package nl.idgis.publisher.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.provider.mock.DatabaseMock;
import nl.idgis.publisher.provider.mock.MetadataMock;
import nl.idgis.publisher.provider.mock.messages.PutMetadata;
import nl.idgis.publisher.provider.mock.messages.PutTable;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.Column;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.DatasetNotAvailable;
import nl.idgis.publisher.provider.protocol.DatasetNotFound;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.Ask;
import nl.idgis.publisher.utils.Ask.Response;

import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.util.Timeout;

public class ProviderTest {
	
	private static final FiniteDuration duration = FiniteDuration.create(1, TimeUnit.SECONDS);
	
	private static final Timeout timeout = new Timeout(duration);
	
	private ActorSystem actorSystem;
	
	private ActorRef sender, recorder, provider;
	
	private ActorSelection metadata, database;
	
	private MetadataDocument metadataDocument;
	
	@Before
	public void actors() {
		actorSystem = ActorSystem.create("test");
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");
		provider = actorSystem.actorOf(Provider.props(DatabaseMock.props(recorder), MetadataMock.props(recorder)), "provider");
		
		metadata = ActorSelection.apply(provider, "metadata");
		database = ActorSelection.apply(provider, "database");
	}
	
	@Before
	public void metadataDocument() throws Exception {
		InputStream inputStream = ProviderTest.class.getResourceAsStream("metadata.xml");
		assertNotNull(inputStream);
		
		byte[] content = IOUtils.toByteArray(inputStream);
		MetadataDocumentFactory metadataDocumentFactory = new MetadataDocumentFactory();
		metadataDocument = metadataDocumentFactory.parseDocument(content);
	}
	
	private <T> T ask(Object msg, Class<T> expected) throws Exception {
		return ask(provider, msg, expected);
	}
	
	private Response askResponse(ActorRef actorRef, Object msg) throws Exception {
		Response response = Await.result(Ask.askResponse(actorSystem, actorRef, msg, timeout), duration);
		sender = response.getSender();
		return response;
	}
	
	private Response askResponse(ActorSelection actorSelection, Object msg) throws Exception {
		Response response = Await.result(Ask.askResponse(actorSystem, actorSelection, msg, timeout), duration);
		sender = response.getSender();
		return response;
	}
	
	private <T> T ask(ActorRef actorRef, Object msg, Class<T> expected) throws Exception {
		return result(expected, askResponse(actorRef, msg).getMessage());
	}
	
	private <T> T ask(ActorSelection actorSelection, Object msg, Class<T> expected) throws Exception {
		return result(expected, askResponse(actorSelection, msg).getMessage());
	}

	private <T> T result(Class<T> expected, Object result) throws Exception {
		if(expected.isInstance(result)) {
			return expected.cast(result);
		} else {
			fail("expected: " + expected.getCanonicalName() + " received: " + result.getClass().getCanonicalName());
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Iterator<RecordedMessage> playRecording() throws Exception {
		return ask(recorder, new GetRecording(), Iterable.class).iterator();
	}
	
	private void clearRecording() throws Exception {
		ask(recorder, new Clear(), Ack.class);
	}

	@Test
	public void testListDatasetInfo() throws Exception {
		Set<AttachmentType> attachmentTypes = new HashSet<>();
		attachmentTypes.add(AttachmentType.METADATA);
		
		ask(new ListDatasetInfo(attachmentTypes), End.class);
		
		assertDatabaseInteractions();
		
		final String firstTableName = ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
		ask(metadata, new PutMetadata("first", metadataDocument.getContent()), Ack.class);
		
		clearRecording();
		
		UnavailableDatasetInfo unavailableDatasetInfo = ask(new ListDatasetInfo(attachmentTypes), UnavailableDatasetInfo.class);
		assertEquals("first", unavailableDatasetInfo.getIdentification());
		
		ask(sender, new NextItem(), End.class);
		 
		assertDatabaseInteractions(firstTableName);
		
		Column[] columns = new Column[]{new Column("id", Type.NUMERIC), new Column("title", Type.TEXT)};
		TableDescription tableDescription = new TableDescription(columns);		
				
		List<Record> records = new ArrayList<>();
		for(int i = 0; i < 42; i++) {
			records.add(new Record(Arrays.<Object>asList(i, "title" + i)));
		}
		ask(database, new PutTable(firstTableName, tableDescription, records), Ack.class);
		
		clearRecording();
		
		VectorDatasetInfo vectorDatasetInfo = ask(new ListDatasetInfo(attachmentTypes), VectorDatasetInfo.class);
		assertEquals("first", vectorDatasetInfo.getIdentification());
		assertEquals(tableDescription, vectorDatasetInfo.getTableDescription());
		assertEquals(42, vectorDatasetInfo.getNumberOfRecords());
		
		ask(sender, new NextItem(), End.class);
		
		assertDatabaseInteractions(firstTableName);
		
		metadataDocument.setAlternateTitle("Test_schema.Test_table");
		final String secondTableName = ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
		
		assertEquals("test_schema.test_table", secondTableName);
		
		ask(metadata, new PutMetadata("second", metadataDocument.getContent()), Ack.class);
		
		clearRecording();
		
		DatasetInfo datasetInfo = ask(new ListDatasetInfo(attachmentTypes), VectorDatasetInfo.class);
		assertEquals("first", datasetInfo.getIdentification());
		
		datasetInfo = ask(sender, new NextItem(), UnavailableDatasetInfo.class);
		assertEquals("second", datasetInfo.getIdentification());
		
		ask(sender, new NextItem(), End.class);
		
		assertDatabaseInteractions(firstTableName, secondTableName);
	}
	
	@Test
	public void testGetDatasetInfo() throws Exception {
		Set<AttachmentType> attachmentTypes = new HashSet<>();
		attachmentTypes.add(AttachmentType.METADATA);
		
		DatasetNotFound notFound = ask(new GetDatasetInfo(attachmentTypes, "test"), DatasetNotFound.class);
		assertEquals("test", notFound.getIdentification());
		
		ask(metadata, new PutMetadata("test", metadataDocument.getContent()), Ack.class);
		
		UnavailableDatasetInfo unavailableDatasetInfo = ask(new GetDatasetInfo(attachmentTypes, "test"), UnavailableDatasetInfo.class);
		assertEquals("test", unavailableDatasetInfo.getIdentification());
		
		Column[] columns = new Column[]{new Column("id", Type.NUMERIC), new Column("geometry", Type.GEOMETRY)};
		TableDescription tableDescription = new TableDescription(columns);
		
		final String tableName = ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
		List<Record> records = new ArrayList<>();
		for(int i = 0; i < 42; i++) {
			records.add(new Record(Arrays.<Object>asList(i, "title" + i)));
		}
		ask(database, new PutTable(tableName, tableDescription, records), Ack.class);
		
		VectorDatasetInfo vectorDatasetInfo = ask(new GetDatasetInfo(attachmentTypes, "test"), VectorDatasetInfo.class);
		assertEquals("test", vectorDatasetInfo.getIdentification());
		assertEquals(42, vectorDatasetInfo.getNumberOfRecords());
		assertEquals(tableDescription, vectorDatasetInfo.getTableDescription());
	}
	
	@Test
	public void testGetVectorDataset() throws Exception {
		GetVectorDataset getVectorDataset = new GetVectorDataset("test", Arrays.asList("id", "title"), 10);
		DatasetNotFound notFound = ask(getVectorDataset, DatasetNotFound.class);
		assertEquals("test", notFound.getIdentification());
		
		ask(metadata, new PutMetadata("test", metadataDocument.getContent()), Ack.class);
		
		DatasetNotAvailable notAvailable = ask(getVectorDataset, DatasetNotAvailable.class);
		assertEquals("test", notAvailable.getIdentification());
		
		Column[] columns = new Column[]{new Column("id", Type.NUMERIC), new Column("title", Type.TEXT)};
		TableDescription tableDescription = new TableDescription(columns);
		
		final String tableName = ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
		List<Record> records = new ArrayList<>();
		for(int i = 0; i < 42; i++) {
			records.add(new Record(Arrays.<Object>asList(i, "title" + i)));
		}
		ask(database, new PutTable(tableName, tableDescription, records), Ack.class);
		
		Records resultRecords = ask(getVectorDataset, Records.class);
		for(int i = 0; i < 4; i++) {			
			assertEquals(10, resultRecords.getRecords().size());
			resultRecords = ask(sender, new NextItem(), Records.class);
		}
		
		assertEquals(2, resultRecords.getRecords().size());
		ask(sender, new NextItem(), End.class);
	}

	private void assertDatabaseInteractions(final String... tableNames) throws Exception {
		Iterator<RecordedMessage> recording = playRecording();
		
		RecordedMessage record;
		assertTrue(recording.hasNext());
		
		record = recording.next();
		assertTrue(record.getMessage() instanceof GetAllMetadata);
		
		for(String tableName : tableNames) {
		
			assertTrue(recording.hasNext());
			
			record = recording.next();
			Object message = record.getMessage(); 
			assertTrue(message instanceof DescribeTable);
			
			DescribeTable describeTable = (DescribeTable)message;
			assertEquals(tableName, describeTable.getTableName());
			
			assertTrue(recording.hasNext());
			
			record = recording.next();
			message = record.getMessage();
			assertTrue(message instanceof PerformCount);
			
			PerformCount performCount = (PerformCount)message;
			assertEquals(tableName, performCount.getTableName());
		}
		
		assertFalse(recording.hasNext());
	}
}
