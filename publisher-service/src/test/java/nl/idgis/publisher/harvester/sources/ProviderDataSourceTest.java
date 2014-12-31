package nl.idgis.publisher.harvester.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.collector.Collector;
import nl.idgis.publisher.collector.messages.GetMessage;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.harvester.sources.mock.ProviderMock;
import nl.idgis.publisher.harvester.sources.mock.messages.PutDataset;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.ColumnInfo;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.provider.protocol.TableInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.SyncAskHelper;

public class ProviderDataSourceTest {
	
	static VectorDatasetInfo vectorDatasetInfo;
	
	ActorSystem actorSystem;
	
	ActorRef recorder, provider, providerDataSource;
	
	SyncAskHelper sync;
	
	@BeforeClass
	public static void initStatics() throws Exception {
		InputStream inputStream = ProviderDataSourceTest.class.getResourceAsStream("/nl/idgis/publisher/metadata/dataset.xml");
		assertNotNull("metadata document missing", inputStream);
		
		byte[] metadataContent = IOUtils.toByteArray(inputStream);
		
		Set<Attachment> attachments = new HashSet<>();
		attachments.add(new Attachment("metadata", AttachmentType.METADATA, metadataContent));
		
		Set<Log> logs = new HashSet<>();
		
		ColumnInfo[] columns = {new ColumnInfo("id", Type.NUMERIC), new ColumnInfo("title", Type.TEXT)};
		TableInfo tableInfo = new TableInfo(columns);
		
		vectorDatasetInfo = new VectorDatasetInfo("vectorDataset", "vectorDatasetTitle", "categoryId", new Date(), attachments, logs, "tableName", tableInfo, 42);				
	}
	
	@Before
	public void actorSystem() {
		Config config = ConfigFactory.empty().withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));		
		actorSystem = ActorSystem.create("test", config);
		
		recorder = actorSystem.actorOf(Recorder.props(), "recorder");
		
		provider = actorSystem.actorOf(ProviderMock.props(recorder), "providerMock");		
		providerDataSource = actorSystem.actorOf(ProviderDataSource.props(provider), "providerDataSource");
		
		sync = new SyncAskHelper(actorSystem);
	}
	
	@After
	public void stopActorSystem() {
		actorSystem.shutdown();
	}
	
	@Test
	public void testListDatasets() throws Exception {
		sync.ask(providerDataSource, new ListDatasets(), End.class);		
		
		sync.ask(provider, new PutDataset(vectorDatasetInfo), Ack.class);
		
		VectorDataset dataset = sync.ask(providerDataSource, new ListDatasets(), VectorDataset.class);
		
		Table table = dataset.getTable();
		assertNotNull(table);
		
		List<Column> columns = table.getColumns();
		assertNotNull(columns);
		
		Iterator<Column> columnsItr = columns.iterator();
		assertTrue(columnsItr.hasNext());
		
		Column column = columnsItr.next();
		assertNotNull(column);
		assertEquals("id", column.getName());		
		assertEquals(Type.NUMERIC, column.getDataType());
		
		assertTrue(columnsItr.hasNext());
		assertNotNull(columnsItr.next());
		
		assertFalse(columnsItr.hasNext());
		
		sync.askSender(new NextItem(), End.class);
	}
	
	@Test
	public void testGetDatasetMetadata() throws Exception {
		sync.ask(provider, new PutDataset(vectorDatasetInfo), Ack.class);		
		sync.ask(providerDataSource, new GetDatasetMetadata("vectorDataset"), MetadataDocument.class);		
	}
	
	public static class DatasetReceiver extends UntypedActor {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		private final ActorRef recordsCollector, startImportCollector;
		
		private final List<Record> records = new ArrayList<>();
		
		public DatasetReceiver(ActorRef recordsCollector, ActorRef startImportCollector) {
			this.recordsCollector = recordsCollector;
			this.startImportCollector = startImportCollector;
		}
		
		static Props props(ActorRef recordsCollector, ActorRef startImportCollector) {
			return Props.create(DatasetReceiver.class, recordsCollector, startImportCollector);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof StartImport) {
				log.debug("start import received");
				
				startImportCollector.forward(msg, getContext());
				
				getSender().tell(new Ack(), getSelf());
			} else if(msg instanceof Records) {
				log.debug("records received");
				
				records.addAll(((Records)msg).getRecords());
				getSender().tell(new NextItem(), getSelf());
			} else if(msg instanceof End) {
				log.debug("end received");
				
				recordsCollector.tell(records, getSelf());
				
				getContext().stop(getSelf());
			} else {
				unhandled(msg);
			}
		}
		
	}
	
	@Test
	public void testGetDataset() throws Exception {
		Set<Record> records = new HashSet<>();
		for(int i = 0; i < 42; i++) {
			records.add(new Record(Arrays.<Object>asList(i, "title" + i)));
		}
		
		sync.ask(provider, new PutDataset(vectorDatasetInfo, records), Ack.class);
		
		ActorRef sessionInitiator = actorSystem.actorOf(Collector.props(), "session-initiator");		
		
		ActorRef startImportCollector = actorSystem.actorOf(Collector.props(), "start-import-collector");		
		ActorRef recordsCollector = actorSystem.actorOf(Collector.props(), "records-collector");		
		providerDataSource.tell(new GetDataset("vectorDataset", Arrays.asList("id", "title"), DatasetReceiver.props(recordsCollector, startImportCollector)), sessionInitiator);
		
		StartImport startImport = sync.ask(startImportCollector, new GetMessage(), StartImport.class);
		assertEquals(startImport.getInitiator(), sessionInitiator);
		
		List<?> returnedRecords = sync.ask(recordsCollector, new GetMessage(), List.class);
		assertEquals(42, returnedRecords.size());
	}
}
