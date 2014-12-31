package nl.idgis.publisher.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.provider.metadata.messages.GetMetadata;
import nl.idgis.publisher.provider.mock.DatabaseMock;
import nl.idgis.publisher.provider.mock.MetadataMock;
import nl.idgis.publisher.provider.mock.messages.PutMetadata;
import nl.idgis.publisher.provider.mock.messages.PutTable;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.DatasetNotAvailable;
import nl.idgis.publisher.provider.protocol.DatasetNotFound;
import nl.idgis.publisher.provider.protocol.EchoRequest;
import nl.idgis.publisher.provider.protocol.EchoResponse;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.RecordedMessage;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.SyncAskHelper;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.japi.Procedure;

public class ProviderTest {
	
	private static TableInfo tableInfo;
	
	private static List<Record> tableContent;
	
	private static Set<AttachmentType> metadataType;
		
	private ActorRef recorder, provider;
	
	private ActorSelection metadata, database;
	
	private SyncAskHelper sync;
	
	private MetadataDocument metadataDocument;
	
	@BeforeClass
	public static void initStatics() {
		ColumnInfo[] columns = new ColumnInfo[]{new ColumnInfo("id", Type.NUMERIC), new ColumnInfo("title", Type.TEXT)};
		tableInfo = new TableInfo(columns);
		
		tableContent = new ArrayList<>();
		for(int i = 0; i < 42; i++) {
			tableContent.add(new Record(Arrays.<Object>asList(i, "title" + i)));
		}
		
		metadataType = new HashSet<>();
		metadataType.add(AttachmentType.METADATA);
	}
	
	@Before
	public void actorSystem() {
		ActorSystem actorSystem = ActorSystem.create("test");
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");
		provider = actorSystem.actorOf(Provider.props(DatabaseMock.props(recorder), MetadataMock.props(recorder)), "provider");
		
		metadata = ActorSelection.apply(provider, "metadata");
		database = ActorSelection.apply(provider, "database");
		
		sync = new SyncAskHelper(actorSystem);
	}
	
	@Before
	public void metadataDocument() throws Exception {
		InputStream inputStream = ProviderTest.class.getResourceAsStream("/nl/idgis/publisher/metadata/dataset.xml");
		assertNotNull("metadata document missing", inputStream);
		
		byte[] content = IOUtils.toByteArray(inputStream);
		MetadataDocumentFactory metadataDocumentFactory = new MetadataDocumentFactory();
		metadataDocument = metadataDocumentFactory.parseDocument(content);
	}
	
	private static class Recording {
		
		private final Iterator<RecordedMessage> iterator;
		
		Recording(Iterator<RecordedMessage> iterator) {
			this.iterator = iterator;
		}
		
		Recording assertHasNext() {
			assertTrue(iterator.hasNext());
			
			return this;
		}
		
		Recording assertNotHasNext() {
			assertFalse(iterator.hasNext());
			
			return this;
		}		
		
		<T> Recording assertNext(Class<T> clazz) throws Exception {
			return assertNext(clazz, null);
		}
		
		<T> Recording assertNext(Class<T> clazz, Procedure<T> procedure) throws Exception {
			Object val = iterator.next().getMessage();
			
			assertTrue("expected: " + clazz.getCanonicalName() + " was: " 
					+ val.getClass().getCanonicalName(), clazz.isInstance(val));
			
			if(procedure != null) {
				procedure.apply(clazz.cast(val));
			}
			
			return this;
		}
		
		Recording assertDatabaseInteraction(String... tableNames) throws Exception {
			for(final String tableName : tableNames) {
				
					assertHasNext()
					.assertNext(DescribeTable.class, new Procedure<DescribeTable>() {

						@Override
						public void apply(DescribeTable describeTable) throws Exception {						
							assertEquals(tableName, describeTable.getTableName());
						}
					})
					.assertHasNext()
					.assertNext(PerformCount.class, new Procedure<PerformCount>() {

						@Override
						public void apply(PerformCount performCount) throws Exception {
							assertEquals(tableName, performCount.getTableName());						
						}
						
					});
			}
			
			return this;			
		}
	}
	
	@SuppressWarnings("unchecked")
	private Recording replayRecording() throws Exception {
		return new Recording(sync.ask(recorder, new GetRecording(), Iterable.class).iterator());
	}
	
	private void clearRecording() throws Exception {
		sync.ask(recorder, new Clear(), Cleared.class);
	}
	
	private String getTable() throws Exception {
		return ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
	}

	@Test
	public void testListDatasetInfo() throws Exception {
		sync.ask(provider, new ListDatasetInfo(metadataType), End.class);
		
		replayRecording()
			.assertHasNext()
			.assertNext(GetAllMetadata.class)
			.assertDatabaseInteraction()
			.assertNotHasNext();
		
		final String firstTableName = getTable();
		sync.ask(metadata, new PutMetadata("first", metadataDocument.getContent()), Ack.class);
		
		clearRecording();
		
		UnavailableDatasetInfo unavailableDatasetInfo = sync.ask(provider, new ListDatasetInfo(metadataType), UnavailableDatasetInfo.class);
		assertEquals("first", unavailableDatasetInfo.getIdentification());
		
		sync.askSender(new NextItem(), End.class);
		
		replayRecording()
			.assertHasNext()
			.assertNext(GetAllMetadata.class)				
			.assertDatabaseInteraction(firstTableName)
			.assertNotHasNext();
		
		sync.ask(database, new PutTable(firstTableName, tableInfo, tableContent), Ack.class);
		
		clearRecording();
		
		VectorDatasetInfo vectorDatasetInfo = sync.ask(provider, new ListDatasetInfo(metadataType), VectorDatasetInfo.class);
		assertEquals("first", vectorDatasetInfo.getIdentification());
		assertEquals(tableInfo, vectorDatasetInfo.getTableInfo());
		assertEquals(42, vectorDatasetInfo.getNumberOfRecords());
		
		sync.askSender(new NextItem(), End.class);		
		
		replayRecording()
			.assertHasNext()
			.assertNext(GetAllMetadata.class)			
			.assertDatabaseInteraction(firstTableName)
			.assertNotHasNext();
		
		metadataDocument.setAlternateTitle("Test_schema.Test_table");
		final String secondTableName = getTable();
		
		assertEquals("test_schema.test_table", secondTableName);
		
		sync.ask(metadata, new PutMetadata("second", metadataDocument.getContent()), Ack.class);
		
		clearRecording();
		
		DatasetInfo datasetInfo = sync.ask(provider, new ListDatasetInfo(metadataType), VectorDatasetInfo.class);
		assertEquals("first", datasetInfo.getIdentification());
		
		datasetInfo = sync.askSender(new NextItem(), UnavailableDatasetInfo.class);
		assertEquals("second", datasetInfo.getIdentification());
		
		sync.askSender(new NextItem(), End.class);
		
		replayRecording()
			.assertHasNext()
			.assertNext(GetAllMetadata.class)
			.assertDatabaseInteraction(firstTableName, secondTableName)
			.assertNotHasNext();
	}	
	
	@Test
	public void testGetDatasetInfo() throws Exception {
		DatasetNotFound notFound = sync.ask(provider, new GetDatasetInfo(metadataType, "test"), DatasetNotFound.class);
		assertEquals("test", notFound.getIdentification());		
		
		replayRecording()
			.assertHasNext()
			.assertNext(GetMetadata.class, new Procedure<GetMetadata>() {

				@Override
				public void apply(GetMetadata msg) throws Exception {
					assertEquals("test", msg.getIdentification());
				}
				
			})
			.assertDatabaseInteraction()
			.assertNotHasNext();
		
		sync.ask(metadata, new PutMetadata("test", metadataDocument.getContent()), Ack.class);
		
		clearRecording();
		
		UnavailableDatasetInfo unavailableDatasetInfo = sync.ask(provider, new GetDatasetInfo(metadataType, "test"), UnavailableDatasetInfo.class);
		assertEquals("test", unavailableDatasetInfo.getIdentification());		
		
		replayRecording()
			.assertHasNext()
			.assertNext(GetMetadata.class, new Procedure<GetMetadata>() {

				@Override
				public void apply(GetMetadata msg) throws Exception {
					assertEquals("test", msg.getIdentification());
				}
				
			})				
			.assertDatabaseInteraction(getTable())
			.assertNotHasNext();
		
		sync.ask(database, new PutTable(getTable(), tableInfo, tableContent), Ack.class);
		
		VectorDatasetInfo vectorDatasetInfo = sync.ask(provider, new GetDatasetInfo(metadataType, "test"), VectorDatasetInfo.class);
		assertEquals("test", vectorDatasetInfo.getIdentification());
		assertEquals(42, vectorDatasetInfo.getNumberOfRecords());
		assertEquals(tableInfo, vectorDatasetInfo.getTableInfo());
	}
	
	@Test
	public void testGetVectorDataset() throws Exception {
		GetVectorDataset getVectorDataset = new GetVectorDataset("test", Arrays.asList("id", "title"), 10);
		DatasetNotFound notFound = sync.ask(provider, getVectorDataset, DatasetNotFound.class);
		assertEquals("test", notFound.getIdentification());
		
		sync.ask(metadata, new PutMetadata("test", metadataDocument.getContent()), Ack.class);
		
		DatasetNotAvailable notAvailable = sync.ask(provider, getVectorDataset, DatasetNotAvailable.class);
		assertEquals("test", notAvailable.getIdentification());
		
		final String tableName = getTable();		
		sync.ask(database, new PutTable(tableName, tableInfo, tableContent), Ack.class);
		
		Records resultRecords = sync.ask(provider, getVectorDataset, Records.class);
		for(int i = 0; i < 4; i++) {			
			assertEquals(10, resultRecords.getRecords().size());
			resultRecords = sync.askSender(new NextItem(), Records.class);
		}
		
		assertEquals(2, resultRecords.getRecords().size());
		sync.askSender(new NextItem(), End.class);
	}
	
	@Test
	public void testEchoRequest() throws Exception {
		assertEquals("Hello, world!", sync.ask(provider, new EchoRequest("Hello, world!"), EchoResponse.class).getPayload());
	}
	
}
