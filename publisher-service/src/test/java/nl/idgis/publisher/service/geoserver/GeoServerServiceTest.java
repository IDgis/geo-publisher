package nl.idgis.publisher.service.geoserver;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.util.Timeout;

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
			.withValue("url", ConfigValueFactory.fromAnyRef("http://localhost:" + TestServers.JETTY_PORT + "/"))
			.withValue("user", ConfigValueFactory.fromAnyRef("admin"))
			.withValue("password", ConfigValueFactory.fromAnyRef("geoserver"))
			.withValue("schema", ConfigValueFactory.fromAnyRef("staging_data"));
		
		Config databaseConfig = ConfigFactory.empty()
			.withValue("url", ConfigValueFactory.fromAnyRef("jdbc:postgresql://localhost:" + TestServers.PG_PORT + "/test"))
			.withValue("user", ConfigValueFactory.fromAnyRef("postgres"))
			.withValue("password", ConfigValueFactory.fromAnyRef("postgres"));
		
		geoServerService = actorSystem.actorOf(GeoServerService.props(serviceManager, geoserverConfig, databaseConfig));
		
		sync = new SyncAskHelper(actorSystem, Timeout.apply(1, TimeUnit.MINUTES));
	}
	
	@After
	public void stopServers() throws Exception {
		testServers.stop();
	}
	
	private void processNodeList(NodeList nodeList, Set<String> retval) {
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if(node.getNodeType() == Node.TEXT_NODE) {
				sb.append(node.getTextContent());
			} else {
				processNodeList(node.getChildNodes(), retval);
			}
		}
		
		String result = sb.toString().trim();
		if(!result.isEmpty()) {
			retval.add(result);
		}
	}
	
	private Set<String> getText(Document d) {
		Set<String> retval = new HashSet<>();		
		processNodeList(d.getChildNodes(), retval);		
		return retval;
	}
	
	@Test
	public void testSingleLayer() throws Exception {
		DatasetLayer datasetLayer = mock(DatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.getTableName()).thenReturn("myTable");
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
		stmt.execute("create table \"staging_data\".\"myTable\"(\"id\" serial primary key, \"label\" text)");
		stmt.execute("select AddGeometryColumn ('staging_data', 'myTable', 'the_geom', 4326, 'GEOMETRY', 2)");
		stmt.execute("insert into \"staging_data\".\"myTable\"(\"label\", \"the_geom\") select 'Hello, world!', st_geomfromtext('POINT(42.0 47.0)', 4326)");
		
		stmt.close();
		
		connection.close();
		
		sync.ask(geoServerService, new ServiceJobInfo(0, "service"), Ack.class);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		
		Document getFeatureResponse = db.parse("http://localhost:" + TestServers.JETTY_PORT + "/wfs/service?request=GetFeature&service=WFS&version=1.1.0&typeName=myTable");
		t.transform(new DOMSource(getFeatureResponse), new StreamResult(System.out));
		
		assertTrue(getText(getFeatureResponse).contains("Hello, world!"));
		
		Document getCapabilitiesResponse = db.parse("http://localhost:" + TestServers.JETTY_PORT + "/wms/service?request=GetCapabilities&service=WMS&version=1.3.0");
		t.transform(new DOMSource(getCapabilitiesResponse), new StreamResult(System.out));
	}
}
