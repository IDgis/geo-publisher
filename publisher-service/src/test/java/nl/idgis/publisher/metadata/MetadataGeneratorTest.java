package nl.idgis.publisher.metadata;

import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;

public class MetadataGeneratorTest extends AbstractServiceTest {
	
	ActorRef metadataGenerator;
	
	MetadataStore serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget;
	
	@Before
	public void actor() throws Exception {
		
		Config constants = ConfigFactory.empty();
		ActorRef harvester = actorOf(HarvesterMock.props(), "harvester");
		
		serviceMetadataSource = new MetadataStoreMock(f);
		datasetMetadataTarget = new MetadataStoreMock(f);
		serviceMetadataTarget = new MetadataStoreMock(f);
		
		metadataGenerator = actorOf(MetadataGenerator.props(database, serviceManager, harvester, serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget, constants), "metadataGenerator");
	}
	
	
	@Test
	public void testGenerate() throws Exception {		
		String datasetId = "testDataset";
		
		insertDataset(datasetId);
		
		f.ask(metadataGenerator, new GenerateMetadata(), Ack.class).get();
	}
}
