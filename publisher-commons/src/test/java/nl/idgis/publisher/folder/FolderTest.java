package nl.idgis.publisher.folder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import nl.idgis.publisher.folder.Folder;
import nl.idgis.publisher.folder.messages.FetchFile;
import nl.idgis.publisher.folder.messages.FileChunk;
import nl.idgis.publisher.folder.messages.FileNotExists;
import nl.idgis.publisher.folder.messages.FileReceiver;
import nl.idgis.publisher.folder.messages.FileSize;
import nl.idgis.publisher.folder.messages.GetFileReceiver;
import nl.idgis.publisher.folder.messages.GetFileSize;
import nl.idgis.publisher.protocol.messages.Ack;
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
	
	Path tempFolder;
	
	@Before
	public void start() throws Exception {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		tempFolder = Files.createTempDirectory(null);		
		Path testFile = tempFolder.resolve(Paths.get("test.tif"));
		
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
	
	@After
	public void removeFiles() throws Exception {
		Files.walkFileTree(tempFolder, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if(exc != null) {
					throw exc;
				}
				
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
			
		});
	}

	@Test
	public void testNotExists() throws Exception {		
		f.ask(folder, new FetchFile(Paths.get("not-exists.tif")), FileNotExists.class).get();
	}
	
	@Test
	public void testFetch() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		
		AskResponse<Object> response = f.askWithSender(folder, new FetchFile(Paths.get("test.tif"))).get();
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
	
	@Test
	public void testGetFileSize() throws Exception {
		FileSize fileSize = f.ask(folder, new GetFileSize(Paths.get("test.tif")), FileSize.class).get();
		assertEquals((long)testFileContent.length, fileSize.getSize());
	}
	
	@Test
	public void testStoreFile() throws Exception {
		ActorRef fileReceiver = f.ask(folder, new GetFileReceiver(Paths.get("test2.tif")), FileReceiver.class).get().getReceiver();
		assertNotNull(fileReceiver);
		
		final int chunkSize = 5120;
		for(int position = 0; position < testFileContent.length; position += chunkSize) {
			FileChunk fileChunk = new FileChunk(Arrays.copyOfRange(testFileContent, position, 
				Math.min(position + chunkSize, testFileContent.length)));
			f.ask(fileReceiver, fileChunk, Ack.class).get();
		}
		
		f.ask(fileReceiver, new End(), Ack.class).get();
		
		byte[] bytesWritten = Files.readAllBytes(tempFolder.resolve(Paths.get("test2.tif")));
		assertEquals(testFileContent.length, bytesWritten.length);
		assertTrue(Arrays.equals(bytesWritten, testFileContent));
	}
	
	@Test
	public void testCopy() throws Exception {
		ActorRef fileReceiver = f.ask(folder, new GetFileReceiver(Paths.get("test3.tif")), FileReceiver.class).get().getReceiver();
		assertNotNull(fileReceiver);
		
		AskResponse<Object> response = f.askWithSender(folder, new FetchFile(Paths.get("test.tif"))).get();
		for(;;) {
			Object msg = response.getMessage();
			
			f.ask(fileReceiver, msg, Ack.class).get();
			
			if(msg instanceof FileChunk) {				
				response = f.askWithSender(response.getSender(), new NextItem()).get();
			} else {
				assertTrue(msg instanceof End);
				break;
			}
		}
		
		byte[] bytesWritten = Files.readAllBytes(tempFolder.resolve(Paths.get("test3.tif")));
		assertEquals(testFileContent.length, bytesWritten.length);
		assertTrue(Arrays.equals(bytesWritten, testFileContent));
	}
}
