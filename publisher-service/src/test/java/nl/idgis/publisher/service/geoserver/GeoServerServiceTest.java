package nl.idgis.publisher.service.geoserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.util.Timeout;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.GetService;
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
	
	DocumentBuilder documentBuilder;
		
	XPath xpath;
	
	@Before
	public void xml() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		documentBuilder = dbf.newDocumentBuilder();
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("wms", "http://www.opengis.net/wms");
		
		XPathFactory xf = XPathFactory.newInstance();
		xpath = xf.newXPath();
		xpath.setNamespaceContext(new NamespaceContext() {

			@Override
			public String getNamespaceURI(String prefix) {
				return namespaces.get(prefix);
			}

			@Override
			public String getPrefix(String namespaceURI) {
				return namespaces.inverse().get(namespaceURI);
			}

			@Override
			public Iterator<?> getPrefixes(String namespaceURI) {
				return Arrays.asList(getPrefix(namespaceURI)).iterator();
			}
			
		});
	}
	
	@Before
	public void actors() throws Exception {
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
		
		sync = new SyncAskHelper(actorSystem, Timeout.apply(30, TimeUnit.SECONDS));
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + TestServers.PG_PORT + "/test", "postgres", "postgres");
		
		Statement stmt = connection.createStatement();
		stmt.execute("create schema \"staging_data\"");
		stmt.execute("create table \"staging_data\".\"myTable\"(\"id\" serial primary key, \"label\" text)");
		stmt.execute("select AddGeometryColumn ('staging_data', 'myTable', 'the_geom', 4326, 'GEOMETRY', 2)");
		stmt.execute("insert into \"staging_data\".\"myTable\"(\"label\", \"the_geom\") select 'Hello, world!', st_geomfromtext('POINT(42.0 47.0)', 4326)");
		
		stmt.close();
		
		connection.close();
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
	
	private Set<String> getText(Node node) {
		return getText(node.getChildNodes());
	}
	
	private Set<String> getText(NodeList nodeList) {
		Set<String> retval = new HashSet<>();		
		processNodeList(nodeList, retval);		
		return retval;
	}
	
	private NodeList getNodeList(String expression, Node node) throws Exception {
		return (NodeList)xpath.evaluate(expression, node, XPathConstants.NODESET);
	}
	
	private String getText(String expression, Node node) throws Exception {
		NodeList nodeList = getNodeList(expression, node);		
		
		if(nodeList.getLength() == 0) {
			fail("no result");
		}
		
		if(nodeList.getLength() > 1) {
			fail("multiple results");
		}
		
		return nodeList.item(0).getTextContent();		
	}
	
	@Test
	public void testSingleLayer() throws Exception {
		DatasetLayer datasetLayer = mock(DatasetLayer.class);
		when(datasetLayer.getName()).thenReturn("layer");
		when(datasetLayer.getTitle()).thenReturn("title");
		when(datasetLayer.getAbstract()).thenReturn("abstract");
		when(datasetLayer.getTableName()).thenReturn("myTable");
		when(datasetLayer.isGroup()).thenReturn(false);
		when(datasetLayer.asDataset()).thenReturn(datasetLayer);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(datasetLayer));
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		sync.ask(geoServerService, new ServiceJobInfo(0, "service"), Ack.class);
		
		Document getFeatureResponse = documentBuilder.parse("http://localhost:" + TestServers.JETTY_PORT + "/wfs/service?request=GetFeature&service=WFS&version=1.1.0&typeName=layer");		
		
		assertTrue(getText(getFeatureResponse).contains("Hello, world!"));
		
		Document getCapabilitiesResponse = documentBuilder.parse("http://localhost:" + TestServers.JETTY_PORT + "/service/wms?request=GetCapabilities&service=WMS&version=1.3.0");

		assertEquals("layer", getText("//wms:Layer/wms:Name", getCapabilitiesResponse));
		assertEquals("title", getText("//wms:Layer[wms:Name = 'layer']/wms:Title", getCapabilitiesResponse));
		assertEquals("abstract", getText("//wms:Layer[wms:Name = 'layer']/wms:Abstract", getCapabilitiesResponse));
	}
	
	@Test
	public void testGroupLayer() throws Exception {
		final int numberOfLayers = 10;
		
		List<Layer> layers = new ArrayList<>();
		for(int i = 0; i < numberOfLayers; i++) {
			DatasetLayer layer = mock(DatasetLayer.class);
			when(layer.isGroup()).thenReturn(false);
			when(layer.asDataset()).thenReturn(layer);
			when(layer.getName()).thenReturn("layer" + i);
			when(layer.getTableName()).thenReturn("myTable");
			
			layers.add(layer);
		}
		
		GroupLayer groupLayer = mock(GroupLayer.class);
		when(groupLayer.isGroup()).thenReturn(true);
		when(groupLayer.asGroup()).thenReturn(groupLayer);
		when(groupLayer.getName()).thenReturn("group");
		when(groupLayer.getTitle()).thenReturn("groupTitle");
		when(groupLayer.getAbstract()).thenReturn("groupAbstract");
		when(groupLayer.getLayers()).thenReturn(layers);
		
		Service service = mock(Service.class);
		when(service.getId()).thenReturn("service");
		when(service.getRootId()).thenReturn("root");
		when(service.getLayers()).thenReturn(Collections.singletonList(groupLayer));
		
		sync.ask(serviceManager, new PutService("service", service), Ack.class);
		
		sync.ask(geoServerService, new ServiceJobInfo(0, "service"), Ack.class);
		
		Document getCapabilitiesResponse = documentBuilder.parse("http://localhost:" + TestServers.JETTY_PORT + "/service/wms?request=GetCapabilities&service=WMS&version=1.3.0");
		
		Set<String> layerNames = getText(getNodeList("//wms:Layer/wms:Name", getCapabilitiesResponse));
		for(int i = 0; i < numberOfLayers; i++) {
			assertTrue(layerNames.contains("service:layer" + i)); // TODO: figure out how to remove the workspace from the name
		}
		assertTrue(layerNames.contains("group"));
		
		layerNames = getText(getNodeList("//wms:Layer[wms:Name = 'group']/wms:Layer/wms:Name", getCapabilitiesResponse));
		for(int i = 0; i < numberOfLayers; i++) {
			assertTrue(layerNames.contains("service:layer" + i));
		}
		assertFalse(layerNames.contains("group"));
		
		assertEquals("groupTitle", getText("//wms:Layer[wms:Name = 'group']/wms:Title", getCapabilitiesResponse));
		assertEquals("groupAbstract", getText("//wms:Layer[wms:Name = 'group']/wms:Abstract", getCapabilitiesResponse));
	}
}
