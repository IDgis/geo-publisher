package nl.idgis.publisher.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class MetadataTargetTest {
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	FileSystem fileSystem;
	
	Path serviceMetadataDirectory, datasetMetadataDirectory;
	
	ActorRef metadataTarget;
	
	Set<Path> tempDirectories;

	@Before
	public void start() throws Exception {
		actorSystem =ActorSystem.create();
		f = new FutureUtils(actorSystem);
		
		fileSystem = Jimfs.newFileSystem(Configuration.unix());
		serviceMetadataDirectory = fileSystem.getPath("/service-metadata");
		Files.createDirectory(serviceMetadataDirectory);
		
		datasetMetadataDirectory = fileSystem.getPath("/dataset-metadata");
		Files.createDirectory(datasetMetadataDirectory);
		
		tempDirectories = new HashSet<>();
		
		metadataTarget = actorSystem.actorOf(
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
}
