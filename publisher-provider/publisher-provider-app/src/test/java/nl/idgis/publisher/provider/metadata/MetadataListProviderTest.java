package nl.idgis.publisher.provider.metadata;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Unavailable;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class MetadataListProviderTest {
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	@After
	public void shutdown() {
		actorSystem.shutdown();
	}

	@Before
	public void actorSystem() {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		f = new FutureUtils(actorSystem);
	}
	
	@Test
	public void testDirectoryUnavailable() throws Exception {
		File tmpDir = Files.createTempDirectory("metadata-test").toFile();
		
		ActorRef metadataListProvider = actorSystem.actorOf(MetadataListProvider.props(tmpDir), "metadata-list-provider");
		f.ask(metadataListProvider, new GetAllMetadata(), End.class).get();
		
		tmpDir.delete();
		f.ask(metadataListProvider, new GetAllMetadata(), Unavailable.class).get();
		
		assertTrue(tmpDir.mkdir());
		f.ask(metadataListProvider, new GetAllMetadata(), End.class).get();
		
		tmpDir.deleteOnExit();
		tmpDir.delete();
	}
}
