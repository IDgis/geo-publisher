package nl.idgis.publisher.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.util.Timeout;

import nl.idgis.publisher.database.messages.ServiceJobInfo;

import nl.idgis.publisher.job.JobExecutorFacade;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.messages.GetContent;
import nl.idgis.publisher.service.messages.Layer;
import nl.idgis.publisher.service.messages.ServiceContent;
import nl.idgis.publisher.service.messages.VirtualService;
import nl.idgis.publisher.utils.SyncAskHelper;
import nl.idgis.publisher.AbstractServiceTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;

public class ServiceTest extends AbstractServiceTest {
	
	TestServers testServers;
	
	@Before
	public void startServers() throws Exception {
		testServers = new TestServers();
		testServers.start();
	}
	
	@After
	public void stopServers() throws Exception {
		testServers.stop();
	}
	
	ActorRef service;

	@Before
	public void actor() {
		Config geoserverConfig = 
				ConfigFactory.empty()
					.withValue("url", ConfigValueFactory.fromAnyRef("http://localhost:" + TestServers.JETTY_PORT + "/"))
					.withValue("user", ConfigValueFactory.fromAnyRef("admin"))
					.withValue("password", ConfigValueFactory.fromAnyRef("geoserver"));
		
		Config geometryDatabaseConfig = 
				ConfigFactory.empty()
					.withValue("url", ConfigValueFactory.fromAnyRef("jdbc:postgresql://localhost:" + TestServers.PG_PORT + "/test"))
					.withValue("user", ConfigValueFactory.fromAnyRef("postgres"))
					.withValue("password", ConfigValueFactory.fromAnyRef("postgres"));
				
		service = actorOf(JobExecutorFacade.props(jobManager, actorOf(Service.props(geoserverConfig, geometryDatabaseConfig), "service")), "serviceFacade");
	}
	
	@Test
	public void testGetContent() throws Exception {
		ServiceContent serviceContent = sync.ask(service, new GetContent(), ServiceContent.class);
		
		List<VirtualService> services = serviceContent.getServices();
		assertNotNull(services);
		assertTrue(services.isEmpty());
		
		createDataset(0, "public", "test_table");
		createDataset(1, "b0", "another_test_table");
		
		SyncAskHelper syncWaitLonger = new SyncAskHelper(system, Timeout.apply(20000));
		syncWaitLonger.ask(service, new ServiceJobInfo(0, "public", "test_table"), Ack.class);
		syncWaitLonger.ask(service, new ServiceJobInfo(1, "b0", "another_test_table"), Ack.class);
		
		serviceContent = sync.ask(service, new GetContent(), ServiceContent.class);
		
		services = serviceContent.getServices();
		assertNotNull(services);
		assertFalse(services.isEmpty());
		
		Set<String> tableNames = new HashSet<String>();
		for(VirtualService virtualService : services) {
			assertNotNull(virtualService);
			
			List<Layer> layers = virtualService.getLayers();
			assertNotNull(layers);
					
			for(Layer layer : layers) {			
				tableNames.add(layer.getSchemaName() + "." + layer.getTableName());
			}
		}
		
		assertTrue(tableNames.contains("b0.another_test_table"));
		assertTrue(tableNames.contains("public.test_table"));		
	}

	private void createDataset(int id, String schema, String table) {
		int dataSourceId = insertDataSource();
		
		insert(category)
			.set(category.id, id)
			.set(category.identification, schema)
			.set(category.name, schema)
			.execute();
		
		insert(sourceDataset)
			.set(sourceDataset.id, id)
			.set(sourceDataset.identification, table)
			.set(sourceDataset.dataSourceId, dataSourceId)
			.execute();
		
		insert(sourceDatasetVersion)
			.set(sourceDatasetVersion.id, id)
			.set(sourceDatasetVersion.type, "VECTOR")
			.set(sourceDatasetVersion.sourceDatasetId, id)
			.set(sourceDatasetVersion.categoryId, id)
			.execute();
		
		insert(dataset)
			.set(dataset.sourceDatasetId, id)
			.set(dataset.id, id)
			.set(dataset.name, table)
			.set(dataset.identification, table)
			.set(dataset.uuid, UUID.randomUUID().toString())
			.set(dataset.fileUuid, UUID.randomUUID().toString())
			.execute();
			
		insert(job)
			.set(job.id, id)
			.set(job.type, "SERVICE")
			.execute();
		
		insert(serviceJob)
			.set(serviceJob.jobId, id)
			.set(serviceJob.datasetId, id)
			.set(serviceJob.sourceDatasetVersionId, id)
			.execute();
		
		assertTrue(
			query().from(job)
				.join(serviceJob).on(serviceJob.jobId.eq(job.id))
				.join(dataset).on(dataset.id.eq(serviceJob.datasetId))
				.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
				.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(serviceJob.sourceDatasetVersionId))
				.join(category).on(category.id.eq(sourceDatasetVersion.categoryId))
				.where(dataset.identification.eq(table))
				.where(category.identification.eq(schema))
				.exists());
	}
}
