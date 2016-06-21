package nl.idgis.publisher.provider;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.provider.mock.DatabaseMock;
import nl.idgis.publisher.provider.protocol.UnavailableDatasetInfo;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VectorDatasetInfoBuilderTest {
	
	ActorRef database;
	
	ActorSystem actorSystem;
		
	FutureUtils f;
	
	byte[] metadata;
	
	@Before
	public void actors() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.apply("test", akkaConfig);
		
		ActorRef databaseRecorder = actorSystem.actorOf(Recorder.props(), "database-recorder");		
		database = actorSystem.actorOf(DatabaseMock.props(databaseRecorder), "database");
		
		f = new FutureUtils(actorSystem);
	}
	
	@Before
	public void metadata() throws Exception {
		InputStream inputStream = VectorDatasetInfoBuilderTest.class.getResourceAsStream("metadata.xml");
		assertNotNull(inputStream);
		
		metadata = IOUtils.toByteArray(inputStream);
	}
	
	@Test
	public void testUnavailableConfidential() throws Exception {
		MetadataDocumentFactory mdf = new MetadataDocumentFactory();
		MetadataDocument metadataDocument = mdf.parseDocument(metadata);
		assertNotNull(metadataDocument);
		
		assertEquals(Arrays.asList("alleen voor intern gebruik"), metadataDocument.getOtherConstraints());
		
		ActorRef recorder = actorSystem.actorOf(AnyRecorder.props(), "recorder");		
		ActorRef builder = actorSystem.actorOf(VectorDatasetInfoBuilder.props(database).props(recorder, Collections.emptySet()), "builder");
		
		builder.tell(new MetadataItem("id", metadata), ActorRef.noSender());
		
		f.ask(recorder, new Wait(1), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(UnavailableDatasetInfo.class, (datasetInfo, sender) -> {
				assertTrue(datasetInfo.isConfidential());
			})
			.assertNotHasNext();
	}
}
