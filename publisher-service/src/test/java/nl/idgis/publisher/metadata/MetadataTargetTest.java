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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

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
		actorSystem =ActorSystem.create();
		f = new FutureUtils(actorSystem);
		
		fileSystem = Jimfs.newFileSystem(Configuration.unix());
		serviceMetadataDirectory = fileSystem.getPath("/service-metadata");		
		datasetMetadataDirectory = fileSystem.getPath("/dataset-metadata");
	}
	
	private ActorRef createMetadataTarget(Set<Path> tempDirectories) {
		return createMetadataTarget(serviceMetadataDirectory, datasetMetadataDirectory, tempDirectories);
	}

	private ActorRef createMetadataTarget(Path serviceMetadataDirectory, Path datasetMetadataDirectory, Set<Path> tempDirectories) {
		return actorSystem.actorOf(
			MetadataTarget.props(
				serviceMetadataDirectory, 
				datasetMetadataDirectory,
				() -> {
					Path retval = fileSystem.getPath("/temp").resolve("" + tempDirectories.size());
					tempDirectories.add(retval);
					
					return retval;
				}), 
			"metadata-target");
	}
	
	@After
	public void stop() {
		actorSystem.shutdown();
	}
	
	@Test
	public void testPutServiceMetadata() throws Exception {
		Path targetFile = serviceMetadataDirectory.resolve("serviceId.xml");
		assertFalse(Files.exists(targetFile));
			
		Set<Path> tempDirectories = new HashSet<>();
		ActorRef metadataTarget = createMetadataTarget(tempDirectories);
		
		f.ask(metadataTarget, new BeginPutMetadata(), Ack.class).get();
		
		f.ask(metadataTarget, 
			new PutServiceMetadata(
				"serviceId", 
				MetadataDocumentTest.getDocument("service_metadata.xml")),
			Ack.class).get();
		
		assertFalse(Files.exists(targetFile));
		
		tempDirectories.forEach(tempDirectory ->
			assertTrue(Files.exists(tempDirectory)));
				
		f.ask(metadataTarget, new CommitMetadata(), Ack.class).get();
				
		assertTrue(Files.exists(targetFile));
		
		tempDirectories.forEach(tempDirectory ->
			assertFalse(Files.exists(tempDirectory)));
	}
	
	@Test
	public void testPutDatasetMetadata() throws Exception {
		Path targetFile = datasetMetadataDirectory.resolve("datasetId.xml");
		assertFalse(Files.exists(targetFile));
		
		Set<Path> tempDirectories = new HashSet<>();
		ActorRef metadataTarget = createMetadataTarget(tempDirectories);
		
		f.ask(metadataTarget, new BeginPutMetadata(), Ack.class).get();
		
		f.ask(metadataTarget, 
			new PutDatasetMetadata(
				"datasetId", 
				MetadataDocumentTest.getDocument("dataset_metadata.xml")),
			Ack.class).get();
		
		tempDirectories.forEach(tempDirectory ->
			assertTrue(Files.exists(tempDirectory)));
		
		f.ask(metadataTarget, new CommitMetadata(), Ack.class).get();
		
		assertTrue(Files.exists(targetFile));
		
		tempDirectories.forEach(tempDirectory ->
			assertFalse(Files.exists(tempDirectory)));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNotDirectory() throws IOException {
		Files.createDirectories(datasetMetadataDirectory.getParent());
		
		Writer writer = Files.newBufferedWriter(datasetMetadataDirectory);
		writer.write("Hello, world!");
		writer.close();
		
		createMetadataTarget(new HashSet<>());
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
		
		createMetadataTarget(serviceMetadataDirectory, datasetMetadataDirectory, new HashSet<>());
	}
}
