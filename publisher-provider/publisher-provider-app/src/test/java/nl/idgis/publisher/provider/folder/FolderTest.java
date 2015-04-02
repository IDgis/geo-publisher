package nl.idgis.publisher.provider.folder;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import nl.idgis.publisher.provider.folder.messages.FetchFile;
import nl.idgis.publisher.provider.folder.messages.FileNotExists;
import nl.idgis.publisher.provider.protocol.FileChunk;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.AskResponse;
import nl.idgis.publisher.utils.FutureUtils;

import scala.util.Random;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class FolderTest {
	
	ActorSystem actorSystem;
	
	ActorRef folder;
	
	FutureUtils f;
	
	byte[] testFileContent;
	
	@Before
	public void start() throws Exception {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		Path tempFolder = Files.createTempDirectory(null);		
		Path testFile = tempFolder.resolve(Paths.get("test.tiff"));
		
		testFileContent = new byte[1024 * 1024];
		new Random().nextBytes(testFileContent);
		Files.write(testFile, testFileContent, StandardOpenOption.CREATE_NEW);
		
		folder = actorSystem.actorOf(Folder.props(tempFolder));
		
		f = new FutureUtils(actorSystem);
	}
	
	@After
	public void shutdown() {
		actorSystem.shutdown();
	}	

	@Test
	public void testNotExists() throws Exception {		
		f.ask(folder, new FetchFile(Paths.get("not-exists.tiff")), FileNotExists.class).get();
	}
	
	@Test
	public void testFetch() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		AskResponse<Object> response = f.askWithSender(folder, new FetchFile(Paths.get("test.tiff"))).get();
		for(;;) {
			Object msg = response.getMessage();
			if(msg instanceof FileChunk) {
				stream.write(((FileChunk) msg).getContent());
				response = f.askWithSender(response.getSender(), new NextItem()).get();
			} else {
				assertTrue(msg instanceof End);
				break;
			}
		}
		
		byte[] bytes = stream.toByteArray();
		assertTrue(Arrays.equals(bytes, testFileContent));
		
		stream.close();
	}
}
