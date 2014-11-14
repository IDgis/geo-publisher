package nl.idgis.publisher.metadata;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;

import nl.idgis.publisher.job.messages.CreateImportJob;
import nl.idgis.publisher.job.messages.CreateServiceJob;
import nl.idgis.publisher.job.messages.GetImportJobs;
import nl.idgis.publisher.job.messages.GetServiceJobs;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.AbstractServiceTest;

import static nl.idgis.publisher.utils.TestPatterns.askAssert;

public class MetadataGeneratorTest extends AbstractServiceTest {
	
	ActorRef metadataGenerator;
	
	@Before
	public void actor() {
		metadataGenerator = actorOf(MetadataGenerator.props(database, ActorRef.noSender()), "metadataGenerator");
	}
	
	@Test
	public void testGenerate() throws Exception {
		String datasetId = "testDataset";
		
		insertDataset(datasetId);
		
		askAssert(jobManager, new CreateImportJob(datasetId), Ack.class);
		executeJobs(new GetImportJobs());
		
		askAssert(jobManager, new CreateServiceJob(datasetId), Ack.class);
		executeJobs(new GetServiceJobs());
		
		askAssert(metadataGenerator, new GenerateMetadata(), Ack.class);
	}
}
