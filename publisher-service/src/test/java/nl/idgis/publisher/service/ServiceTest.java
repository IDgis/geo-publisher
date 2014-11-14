package nl.idgis.publisher.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.database.messages.ServiceJobInfo;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.messages.GetContent;
import nl.idgis.publisher.service.messages.Layer;
import nl.idgis.publisher.service.messages.ServiceContent;
import nl.idgis.publisher.service.messages.VirtualService;
import nl.idgis.publisher.AbstractServiceTest;

import static nl.idgis.publisher.utils.TestPatterns.askAssert;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
	
	static class DatabaseMock extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			getSender().tell(new Ack(), getSelf());
		}
		
	}

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
		
		
		ActorRef databaseMock = actorOf(Props.create(DatabaseMock.class), "databaseMock");		
		service = actorOf(Service.props(databaseMock, geoserverConfig, geometryDatabaseConfig), "service");
	}
	
	@Test
	public void testGetContent() throws Exception {
		ServiceContent serviceContent = askAssert(service, new GetContent(), ServiceContent.class);
		
		List<VirtualService> services = serviceContent.getServices();
		assertNotNull(services);
		assertTrue(services.isEmpty());
		
		askAssert(service, new ServiceJobInfo(0, "public", "test_table"), 20000, Ack.class);
		askAssert(service, new ServiceJobInfo(0, "b0", "another_test_table"), 20000, Ack.class);
		
		serviceContent = askAssert(service, new GetContent(), ServiceContent.class);
		
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
}
