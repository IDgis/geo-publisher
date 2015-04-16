package nl.idgis.publisher.metadata;

import java.io.File;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.dataset.DatasetManager;
import nl.idgis.publisher.harvester.Harvester;
import nl.idgis.publisher.harvester.sources.ProviderDataSource;
import nl.idgis.publisher.job.manager.JobManager;
import nl.idgis.publisher.job.manager.messages.CreateEnsureServiceJob;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.job.manager.messages.GetServiceJobs;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;

import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class MetadataGeneratorTest extends AbstractServiceTest {
	
	ActorRef metadataGenerator;
	
	
	@Before
	public void actor() throws Exception {
		// TODO: repair bootstrapping
		
		
		String name = "service";
		final Config defaultConf = ConfigFactory.load();
		
		final Config conf;
		final File confFile = new File("C:\\Projects\\ov-service\\service.conf");
		if(confFile.exists()) {
			conf = ConfigFactory.parseFile(confFile).withFallback(defaultConf);
		} else {
			conf = defaultConf;
		}

		Config serviceConfig = conf.getConfig("publisher." + name);
		
		Config metadataConfig = serviceConfig.getConfig("metadata");
		System.err.println("metadataConfig: " + metadataConfig.toString());
		
		MetadataStore serviceMetadataSource = new FileMetadataStore(new File(metadataConfig.getString("serviceSource")));
		MetadataStore datasetMetadataTarget = new FileMetadataStore(new File(metadataConfig.getString("datasetTarget")));
		MetadataStore serviceMetadataTarget = new FileMetadataStore(new File(metadataConfig.getString("serviceTarget")));		
		
		final ActorRef datasetManager = actorOf(DatasetManager.props(database), "dataset-manager");
		
		final Config harvesterConfig = serviceConfig.getConfig("harvester");
		
		final ActorRef harvester = actorOf(Harvester.props(database, datasetManager, harvesterConfig), "harvester");
		
		metadataGenerator = actorOf(MetadataGenerator.props(database, harvester, serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget, metadataConfig.getConfig("generator-constants")), "metadataGenerator");
	}
	
	
//	@Test
	public void testGenerate() throws Exception {
		// TODO: implement a working test
		System.err.println("Test" );
		String datasetId = "testDataset";
		
		insertDataset(datasetId);
		
		f.ask(jobManager, new CreateImportJob(datasetId), Ack.class).get();
		executeJobs(new GetImportJobs());
		
		f.ask(jobManager, new CreateEnsureServiceJob(datasetId), Ack.class).get();
		executeJobs(new GetServiceJobs());
		
		Ack ack = f.ask(metadataGenerator, new GenerateMetadata(), Ack.class).get();
		
		System.err.println("Ack " + ack);
		
	}
}
