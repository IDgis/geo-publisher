package nl.idgis.publisher.metadata;

import org.junit.Before;

import akka.actor.ActorRef;

import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.CreateEnsureServiceJob;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.job.manager.messages.GetServiceJobs;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.AbstractServiceTest;

public class MetadataGeneratorTest extends AbstractServiceTest {
	
	ActorRef metadataGenerator;
	
	//@Before
	public void actor() {
		// TODO: repair bootstrapping
		
		metadataGenerator = actorOf(MetadataGenerator.props(database, ActorRef.noSender(), ActorRef.noSender(), null, null, null, null), "metadataGenerator");
	}
	
	//@Test
	
	// TODO: implement a working test
	public void testGenerate() throws Exception {
		String datasetId = "testDataset";
		
		insertDataset(datasetId);
		
		sync.ask(jobManager, new CreateImportJob(datasetId), Ack.class);
		executeJobs(new GetImportJobs());
		
		sync.ask(jobManager, new CreateEnsureServiceJob(datasetId), Ack.class);
		executeJobs(new GetServiceJobs());
		
		sync.ask(metadataGenerator, new GenerateMetadata(), Ack.class);
	}
}
