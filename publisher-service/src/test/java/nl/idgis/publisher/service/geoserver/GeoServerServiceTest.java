package nl.idgis.publisher.service.geoserver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.domain.web.NotFound;

import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.DatasetLayer;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.Service;
import nl.idgis.publisher.utils.SyncAskHelper;

public class GeoServerServiceTest {
	
	static class PutService implements Serializable {

		private static final long serialVersionUID = 7974047966502087805L;

		private String serviceId;
		
		private Service service;
		
		public PutService(String serviceId, Service service) {
			this.serviceId = serviceId;
			this.service = service;
		} 
		
		public String getServiceId() {
			return serviceId;			
		}
		
		public Service getService() {
			return service;
		}
	}
	
	static class ServiceManagerMock extends UntypedActor {
		
		private Map<String, Service> services = new HashMap<>();
		
		public static Props props() {
			return Props.create(ServiceManagerMock.class);
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof GetService) {
				String serviceId = ((GetService)msg).getServiceId();
				if(services.containsKey(serviceId)) {
					getSender().tell(services.get(serviceId), getSelf());
				} else {
					getSender().tell(new NotFound(), getSelf());
				}
			} else if(msg instanceof PutService) {
				PutService putService = (PutService)msg;
				services.put(putService.getServiceId(), putService.getService());
				getSender().tell(new Ack(), getSelf());
			} else {
				unhandled(msg);
			}
		}
	}
	
	TestServers testServers;
	
	ActorSystem actorSystem;
	
	ActorRef serviceManager, geoServerService;
	
	SyncAskHelper sync;
	
	@Before
	public void setUp() throws Exception {
		testServers = new TestServers();
		testServers.start();
		
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		ActorSystem actorSystem = ActorSystem.create("test", akkaConfig);
		serviceManager = actorSystem.actorOf(ServiceManagerMock.props(), "service-manager");
		
		Config geoserverConfig = ConfigFactory.empty()
			.withValue("url", ConfigValueFactory.fromAnyRef("http://localhost:" + TestServers.JETTY_PORT + "/geoserver/"))
			.withValue("user", ConfigValueFactory.fromAnyRef("admin"))
			.withValue("password", ConfigValueFactory.fromAnyRef("geoserver"));
		
		Config databaseConfig = ConfigFactory.empty()
			.withValue("url", ConfigValueFactory.fromAnyRef("jdbc:postgresql://localhost:" + TestServers.PG_PORT + "/test"))
			.withValue("user", ConfigValueFactory.fromAnyRef("postgres"))
			.withValue("password", ConfigValueFactory.fromAnyRef("postgres"));
		
		geoServerService = actorSystem.actorOf(GeoServerService.props(serviceManager, geoserverConfig, databaseConfig));
		
		sync = new SyncAskHelper(actorSystem);
	}
	
	@After
	public void stopServers() throws Exception {
		testServers.stop();
	}
	
	@Test
	public void testSingleLayer() throws Exception {
		DatasetLayer datasetLayer = mock(DatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.getTableName()).thenReturn("tableName");
		when(datasetLayer.isGroup()).thenReturn(false);
		when(datasetLayer.asDataset()).thenReturn(datasetLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayer));
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + TestServers.PG_PORT + "/test", "postgres", "postgres");
		
		Statement stmt = connection.createStatement();
		stmt.execute("create schema \"staging_data\"");
		stmt.execute("create table \"staging_data\".\"tableName\"(\"id\" serial, \"test\" integer)");
		stmt.close();
				
		connection.close();
		
		sync.ask(geoServerService, new ServiceJobInfo(0, "service"), Ack.class);
	}
}
