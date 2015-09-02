package nl.idgis.publisher.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import nl.idgis.publisher.metadata.messages.BeginPutMetadata;
import nl.idgis.publisher.metadata.messages.CommitMetadata;
import nl.idgis.publisher.metadata.messages.PutDatasetMetadata;
import nl.idgis.publisher.metadata.messages.PutServiceMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.FutureUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Matchers.anyVararg;

public class MetadataTargetTest {
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	FileSystem fileSystem;
	
	Path serviceMetadataDirectory, datasetMetadataDirectory;

	@Before
	public void start() throws Exception {
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		actorSystem = ActorSystem.create("test", akkaConfig);
		f = new FutureUtils(actorSystem);
		
		fileSystem = Jimfs.newFileSystem(Configuration.unix());
		serviceMetadataDirectory = fileSystem.getPath("/service-metadata");		
		datasetMetadataDirectory = fileSystem.getPath("/dataset-metadata");
	}
	
	private ActorRef createMetadataTarget() {
		return createMetadataTarget(serviceMetadataDirectory, datasetMetadataDirectory);
	}

	private ActorRef createMetadataTarget(Path serviceMetadataDirectory, Path datasetMetadataDirectory) {
		return actorSystem.actorOf(
			MetadataTarget.props(
				serviceMetadataDirectory, 
				datasetMetadataDirectory), 
			"metadata-target");
	}
	
	@After
	public void stop() {
		actorSystem.shutdown();
	}
	
	private Stream<Path> findTempFiles(Path directory) throws IOException {
		return Files.find(directory, 1, (path, attr) -> {
			Path fileName = path.getFileName();
			if(fileName == null) {
				return false;
			}
			
			return fileName.toString().startsWith("_");
		});
	}
	
	@Test
	public void testPutServiceMetadata() throws Exception {
		Path targetFile = serviceMetadataDirectory.resolve("serviceId.xml");
		assertFalse(Files.exists(targetFile));
			
		ActorRef metadataTarget = createMetadataTarget();
		
		assertFalse(findTempFiles(serviceMetadataDirectory.getParent()).findAny().isPresent());
		
		f.ask(metadataTarget, new BeginPutMetadata(), Ack.class).get();
		
		assertTrue(findTempFiles(serviceMetadataDirectory.getParent()).findAny().isPresent());
		
		f.ask(metadataTarget, 
			new PutServiceMetadata(
				"serviceId", 
				MetadataDocumentTest.getDocument("service_metadata.xml")),
			Ack.class).get();
		
		assertFalse(Files.exists(targetFile));
				
		f.ask(metadataTarget, new CommitMetadata(), Ack.class).get();
		
		assertFalse(findTempFiles(serviceMetadataDirectory.getParent()).findAny().isPresent());
				
		assertTrue(Files.exists(targetFile));
	}
	
	@Test
	public void testPutDatasetMetadata() throws Exception {
		Path targetFile = datasetMetadataDirectory.resolve("datasetId.xml");
		assertFalse(Files.exists(targetFile));
		
		ActorRef metadataTarget = createMetadataTarget();
		
		assertFalse(findTempFiles(serviceMetadataDirectory.getParent()).findAny().isPresent());
		
		f.ask(metadataTarget, new BeginPutMetadata(), Ack.class).get();
		
		assertTrue(findTempFiles(serviceMetadataDirectory.getParent()).findAny().isPresent());
		
		f.ask(metadataTarget, 
			new PutDatasetMetadata(
				"datasetId", 
				MetadataDocumentTest.getDocument("dataset_metadata.xml")),
			Ack.class).get();
		
		f.ask(metadataTarget, new CommitMetadata(), Ack.class).get();
		
		assertFalse(findTempFiles(serviceMetadataDirectory.getParent()).findAny().isPresent());
		
		assertTrue(Files.exists(targetFile));		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNotDirectory() throws IOException {
		Files.createDirectories(datasetMetadataDirectory.getParent());
		
		Writer writer = Files.newBufferedWriter(datasetMetadataDirectory);
		writer.write("Hello, world!");
		writer.close();
		
		createMetadataTarget();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCannotCreate() throws Exception {
		FileSystem fileSystem = mock(FileSystem.class);
		
		Path root = mock(Path.class);
		when(root.getFileSystem()).thenReturn(fileSystem);
		
		FileSystemProvider provider = mock(FileSystemProvider.class);		
		when(fileSystem.provider()).thenReturn(provider);
		
		// no paths exists except root
		doThrow(new FileNotFoundException()).when(provider).checkAccess(any(Path.class), anyVararg());
		doNothing().when(provider).checkAccess(same(root), anyVararg());
		
		Path serviceMetadataDirectory = mock(Path.class);
		when(serviceMetadataDirectory.getFileSystem()).thenReturn(fileSystem);
		when(serviceMetadataDirectory.toAbsolutePath()).thenReturn(serviceMetadataDirectory);
		
		Path relativeServiceMetadataDirectory = mock(Path.class);
		when(root.relativize(same(serviceMetadataDirectory))).thenReturn(relativeServiceMetadataDirectory);		
		when(root.resolve(relativeServiceMetadataDirectory)).thenReturn(serviceMetadataDirectory);
		
		// serviceMetadataDirectory is child of root
		when(serviceMetadataDirectory.iterator()).thenReturn(Collections.singleton(relativeServiceMetadataDirectory).iterator());
		when(relativeServiceMetadataDirectory.iterator()).thenReturn(Collections.singleton(relativeServiceMetadataDirectory).iterator());		
		when(serviceMetadataDirectory.getParent()).thenReturn(root);				
		
		doThrow(new AccessDeniedException(null)).when(provider).createDirectory(same(serviceMetadataDirectory), anyVararg());
		
		createMetadataTarget(serviceMetadataDirectory, datasetMetadataDirectory);
	}
}