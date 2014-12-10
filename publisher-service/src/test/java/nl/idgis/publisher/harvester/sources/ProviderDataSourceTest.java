package nl.idgis.publisher.harvester.sources;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Type;

import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.harvester.sources.mock.ProviderMock;
import nl.idgis.publisher.harvester.sources.mock.messages.PutDatasetInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.Attachment;
import nl.idgis.publisher.provider.protocol.Column;
import nl.idgis.publisher.provider.protocol.TableDescription;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.SyncAskHelper;

public class ProviderDataSourceTest {
	
	static VectorDatasetInfo vectorDatasetInfo;
	
	ActorRef recorder, provider, providerDataSource;
	
	SyncAskHelper sync;
	
	@BeforeClass
	public static void initStatics() {
		Set<Attachment> attachments = new HashSet<>();
		Set<Log> logs = new HashSet<>();
		
		Column[] columns = {new Column("id", Type.NUMERIC), new Column("title", Type.TEXT)};
		TableDescription tableDescription = new TableDescription(columns);
		
		vectorDatasetInfo = new VectorDatasetInfo("vectorDataset", "vectorDatasetTitle", attachments, logs, tableDescription, 42);				
	}
	
	@Before
	public void actorSystem() {
		ActorSystem actorSystem = ActorSystem.create("test");
		
		recorder = actorSystem.actorOf(Recorder.props());
		
		provider = actorSystem.actorOf(ProviderMock.props(recorder));		
		providerDataSource = actorSystem.actorOf(ProviderDataSource.props(provider));
		
		sync = new SyncAskHelper(actorSystem);
	}
	
	@Test
	public void testListDatasets() throws Exception {
		sync.ask(providerDataSource, new ListDatasets(), End.class);		
		
		sync.ask(provider, new PutDatasetInfo(vectorDatasetInfo), Ack.class);
		sync.ask(providerDataSource, new ListDatasets(), Dataset.class);
		sync.askSender(new NextItem(), End.class);
	}
}
