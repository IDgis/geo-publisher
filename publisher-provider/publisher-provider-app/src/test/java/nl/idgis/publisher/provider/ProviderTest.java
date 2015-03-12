package nl.idgis.publisher.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

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
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.AskResponse;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;

public class ProviderTest {
	
	private static TableInfo tableInfo;
	
	private static List<Record> tableContent;
	
	private static Set<AttachmentType> metadataType;
		
	private ActorRef recorder, provider;
	
	private ActorSelection metadata, database;
	
	private FutureUtils f;
	
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
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		ActorSystem actorSystem = ActorSystem.create("test", akkaConfig);
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");
		provider = actorSystem.actorOf(Provider.props(DatabaseMock.props(recorder), MetadataMock.props(recorder)), "provider");
		
		metadata = ActorSelection.apply(provider, "metadata");
		database = ActorSelection.apply(provider, "database");
		
		f = new FutureUtils(actorSystem);
	}
	
	@Before
	public void metadataDocument() throws Exception {
		InputStream inputStream = ProviderTest.class.getResourceAsStream("/nl/idgis/publisher/metadata/dataset.xml");
		assertNotNull("metadata document missing", inputStream);
		
		byte[] content = IOUtils.toByteArray(inputStream);
		MetadataDocumentFactory metadataDocumentFactory = new MetadataDocumentFactory();
		metadataDocument = metadataDocumentFactory.parseDocument(content);
	}
	
	private static class DatabaseRecording implements Recording {
		
		private final Recording recording;

		public DatabaseRecording(Recording recording) {
			this.recording = recording;
		}
		
		public DatabaseRecording assertDatabaseInteraction(String... tableNames) throws Exception {
			for(final String tableName : tableNames) {
				assertNext("describe table for: " + tableName, DescribeTable.class, describeTable -> {
					assertEquals(tableName, describeTable.getTableName());						
				})				
				.assertNext("perform count for: " + tableName, PerformCount.class, performCount -> {
					assertEquals(tableName, performCount.getTableName());
				});
			}
			
			return this;			
		}

		@Override
		public DatabaseRecording assertHasNext() {
			recording.assertHasNext();
			
			return this;
		}

		@Override
		public DatabaseRecording assertNotHasNext() {
			recording.assertNotHasNext();
			
			return this;
		}

		@Override
		public <T> DatabaseRecording assertNext(Class<T> clazz) throws Exception {
			recording.assertNext(clazz);

			return this;
		}

		@Override
		public <T> DatabaseRecording assertNext(Class<T> clazz, Consumer<T> procedure) throws Exception {
			recording.assertNext(clazz, procedure);
			
			return this;
		}

		@Override
		public Recording assertHasNext(String message) {
			recording.assertHasNext(message);
			
			return this;
		}

		@Override
		public Recording assertNotHasNext(String message) {
			recording.assertNotHasNext(message);
			
			return this;
		}

		@Override
		public <T> Recording assertNext(String message, Class<T> clazz) throws Exception {
			recording.assertNext(message, clazz);
			
			return this;
		}

		@Override
		public <T> Recording assertNext(String message, Class<T> clazz, Consumer<T> procedure) throws Exception {
			recording.assertNext(message, clazz, procedure);
			
			return this;
		}		
	}
	
	private DatabaseRecording replayRecording(int expectedMessageCount) throws Exception {
		f.ask(recorder, new Wait(expectedMessageCount), Waited.class).get();
		
		return new DatabaseRecording(f.ask(recorder, new GetRecording(), Recording.class).get());
	}
	
	private void clearRecording() throws Exception {
		f.ask(recorder, new Clear(), Cleared.class).get();
	}
	
	private String getTable() throws Exception {
		return ProviderUtils.getTableName(metadataDocument.getAlternateTitle());
	}

	@Test
	public void testListDatasetInfo() throws Exception {
		f.ask(provider, new ListDatasetInfo(metadataType), End.class).get();
		
		replayRecording(1)
			.assertNext(GetAllMetadata.class)			
			.assertNotHasNext();
		
		final String firstTableName = getTable();
		f.ask(metadata, new PutMetadata("first", metadataDocument.getContent()), Ack.class).get();
		
		clearRecording();
		
		AskResponse<UnavailableDatasetInfo> unavailableDatasetInfoWithSender = f.askWithSender(provider, new ListDatasetInfo(metadataType), UnavailableDatasetInfo.class).get();
		
		UnavailableDatasetInfo unavailableDatasetInfo = unavailableDatasetInfoWithSender.getMessage(); 
		assertEquals("first", unavailableDatasetInfo.getIdentification());
		
		f.ask(unavailableDatasetInfoWithSender.getSender(), new NextItem(), End.class);
		
		replayRecording(3)			
			.assertNext(GetAllMetadata.class)				
			.assertDatabaseInteraction(firstTableName)			
			.assertNotHasNext();
		
		f.ask(database, new PutTable(firstTableName, tableInfo, tableContent), Ack.class).get();
		
		clearRecording();
		
		AskResponse<VectorDatasetInfo> vectorDatasetInfoWithSender = f.askWithSender(provider, new ListDatasetInfo(metadataType), VectorDatasetInfo.class).get();
		
		VectorDatasetInfo vectorDatasetInfo = vectorDatasetInfoWithSender.getMessage();
		assertEquals("first", vectorDatasetInfo.getIdentification());
		assertEquals(tableInfo, vectorDatasetInfo.getTableInfo());
		assertEquals(42, vectorDatasetInfo.getNumberOfRecords());
		
		f.ask(vectorDatasetInfoWithSender.getSender(), new NextItem(), End.class);		
		
		replayRecording(3)			
			.assertNext(GetAllMetadata.class)			
			.assertDatabaseInteraction(firstTableName)			
			.assertNotHasNext();
		
		metadataDocument.setAlternateTitle("Test_schema.Test_table");
		final String secondTableName = getTable();
		
		assertEquals("test_schema.test_table", secondTableName);
		
		f.ask(metadata, new PutMetadata("second", metadataDocument.getContent()), Ack.class).get();
		
		clearRecording();
		
		AskResponse<? extends DatasetInfo> datasetInfoWithSender = f.askWithSender(provider, new ListDatasetInfo(metadataType), VectorDatasetInfo.class).get();
		
		DatasetInfo datasetInfo = datasetInfoWithSender.getMessage();
		assertEquals("first", datasetInfo.getIdentification());
		
		datasetInfoWithSender = f.askWithSender(datasetInfoWithSender.getSender(), new NextItem(), UnavailableDatasetInfo.class).get();
		datasetInfo = datasetInfoWithSender.getMessage();
		assertEquals("second", datasetInfo.getIdentification());
		
		f.ask(datasetInfoWithSender.getSender(), new NextItem(), End.class);
		
		replayRecording(5)
			.assertNext(GetAllMetadata.class)
			.assertDatabaseInteraction(firstTableName, secondTableName)
			.assertNotHasNext();
	}	
	
	@Test
	public void testGetDatasetInfo() throws Exception {
		DatasetNotFound notFound = f.ask(provider, new GetDatasetInfo(metadataType, "test"), DatasetNotFound.class).get();
		assertEquals("test", notFound.getIdentification());		
		
		replayRecording(1)
			.assertNext(GetMetadata.class, msg -> {
				assertEquals("test", msg.getIdentification());
			})			
			.assertNotHasNext();
		
		f.ask(metadata, new PutMetadata("test", metadataDocument.getContent()), Ack.class).get();
		
		clearRecording();
		
		UnavailableDatasetInfo unavailableDatasetInfo = f.ask(provider, new GetDatasetInfo(metadataType, "test"), UnavailableDatasetInfo.class).get();
		assertEquals("test", unavailableDatasetInfo.getIdentification());		
		
		replayRecording(3)
			.assertNext(GetMetadata.class, msg -> {				
				assertEquals("test", msg.getIdentification());				
			})				
			.assertDatabaseInteraction(getTable())
			.assertNotHasNext();
		
		f.ask(database, new PutTable(getTable(), tableInfo, tableContent), Ack.class).get();
		
		VectorDatasetInfo vectorDatasetInfo = f.ask(provider, new GetDatasetInfo(metadataType, "test"), VectorDatasetInfo.class).get();
		assertEquals("test", vectorDatasetInfo.getIdentification());
		assertEquals(42, vectorDatasetInfo.getNumberOfRecords());
		assertEquals(tableInfo, vectorDatasetInfo.getTableInfo());
	}
	
	@Test
	public void testGetVectorDataset() throws Exception {
		GetVectorDataset getVectorDataset = new GetVectorDataset("test", Arrays.asList("id", "title"), 10);
		DatasetNotFound notFound = f.ask(provider, getVectorDataset, DatasetNotFound.class).get();
		assertEquals("test", notFound.getIdentification());
		
		f.ask(metadata, new PutMetadata("test", metadataDocument.getContent()), Ack.class).get();
		
		DatasetNotAvailable notAvailable = f.ask(provider, getVectorDataset, DatasetNotAvailable.class).get();
		assertEquals("test", notAvailable.getIdentification());
		
		final String tableName = getTable();		
		f.ask(database, new PutTable(tableName, tableInfo, tableContent), Ack.class).get();
		
		AskResponse<Records> resultRecordsWithSender = f.askWithSender(provider, getVectorDataset, Records.class).get();
		Records resultRecords = resultRecordsWithSender.getMessage();
		for(int i = 0; i < 4; i++) {			
			assertEquals(10, resultRecords.getRecords().size());
			resultRecordsWithSender = f.askWithSender(resultRecordsWithSender.getSender(), new NextItem(), Records.class).get();
			resultRecords = resultRecordsWithSender.getMessage();
		}
		
		assertEquals(2, resultRecords.getRecords().size());
		f.ask(resultRecordsWithSender.getSender(), new NextItem(), End.class);
	}
	
	@Test
	public void testEchoRequest() throws Exception {
		assertEquals("Hello, world!", f.ask(provider, new EchoRequest("Hello, world!"), EchoResponse.class).get().getPayload());
	}
	
}
