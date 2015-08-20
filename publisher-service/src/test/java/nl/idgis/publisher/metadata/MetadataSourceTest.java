package nl.idgis.publisher.metadata;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.metadata.messages.AddDataSource;
import nl.idgis.publisher.metadata.messages.AddMetadataDocument;
import nl.idgis.publisher.metadata.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.messages.GetServiceMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

public class MetadataSourceTest {
	
	ActorSystem actorSystem;
	
	ActorRef harvester, metadataSource;
	
	Path serviceMetadataDirectory;
	
	FutureUtils f;

	@Before
	public void start() throws Exception {
		actorSystem =ActorSystem.create();
		f = new FutureUtils(actorSystem);
		
		harvester = actorSystem.actorOf(HarvesterMock.props(), "harvester");
		
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		serviceMetadataDirectory = fs.getPath("/service-metadata");
		Files.createDirectory(serviceMetadataDirectory);
		
		metadataSource = actorSystem.actorOf(MetadataSource.props(harvester, serviceMetadataDirectory), "metadata-source");
	}
	
	@After
	public void stop() {
		actorSystem.shutdown();
	}
	
	@Test
	public void testGetServiceMetadata() throws Exception {
		
		f.ask(metadataSource, new GetServiceMetadata("serviceId"), Failure.class).get();
		
		IOUtils.copy(
			getClass().getResourceAsStream("service_metadata.xml"),
			Files.newOutputStream(serviceMetadataDirectory.resolve("serviceId.xml")));
		
		f.ask(metadataSource, new GetServiceMetadata("serviceId"), MetadataDocument.class).get();
	}
	
	@Test
	public void testGetDatasetMetadata() throws Exception {
		
		f.ask(metadataSource, new GetDatasetMetadata("dataSourceId", "datasetId"), NotConnected.class).get();
		
		ActorRef dataSource = actorSystem.actorOf(DataSourceMock.props(), "dataSource");
		f.ask(harvester, new AddDataSource("dataSourceId", dataSource), Ack.class).get();
		
		f.ask(
			dataSource, 
			new AddMetadataDocument("datasetId", MetadataDocumentTest.getDocument("dataset_metadata.xml")), 
			Ack.class).get();
		
		f.ask(metadataSource, new GetDatasetMetadata("dataSourceId", "datasetId"), MetadataDocument.class).get();
	}
}
