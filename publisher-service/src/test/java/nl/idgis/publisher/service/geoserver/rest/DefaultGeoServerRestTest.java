package nl.idgis.publisher.service.geoserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nl.idgis.publisher.service.geoserver.GeoServerTestHelper;
import nl.idgis.publisher.service.geoserver.rest.Attribute;
import nl.idgis.publisher.service.geoserver.rest.DataStore;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.FeatureType;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.Logging;

import org.h2.server.pg.PgServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;

public class DefaultGeoServerRestTest {
	
	static LoggingAdapter log = Logging.getLogger();
	
	static GeoServerTestHelper h;
	
	static GeoServerRest service;
	
	static FutureUtils f;
	
	@BeforeClass
	public static void startServers() throws Exception {
		h = new GeoServerTestHelper();
		h.start();
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + GeoServerTestHelper.PG_PORT + "/test", "postgres", "postgres");
		
		Statement stmt = connection.createStatement();
		stmt.execute("create schema \"public\"");
		stmt.execute("create table \"public\".\"test_table\"(\"id\" serial, \"test\" integer)");
		stmt.execute("select AddGeometryColumn ('public', 'test_table', 'the_geom', 4326, 'GEOMETRY', 2)");
		stmt.execute("create schema \"b0\"");
		stmt.execute("create table \"b0\".\"another_test_table\"(\"id\" serial, \"test\" integer)");
		
		stmt.close();
				
		connection.close();
		
		ActorSystem actorSystem = ActorSystem.create();
		f = new FutureUtils(actorSystem.dispatcher());
		service = new DefaultGeoServerRest(f, log, "http://localhost:" + GeoServerTestHelper.JETTY_PORT + "/", "admin", "geoserver");
	}
	
	@After
	public void clean() throws Exception {
		h.clean(f, log);
	}
	
	@AfterClass
	public static void stopServers() throws Exception {
		h.stop();
	}

	@Test
	public void testCreateLayers() throws Exception {
		
		List<Workspace> workspaces = service.getWorkspaces().get();
		assertNotNull(workspaces);
		assertTrue(workspaces.isEmpty());
		
		service.postWorkspace(new Workspace("testWorkspace")).get();
		
		workspaces = service.getWorkspaces().get();
		assertNotNull(workspaces);
		assertEquals(1, workspaces.size());
		
		Workspace workspace = workspaces.get(0);
		assertNotNull(workspace);
		assertEquals("testWorkspace", workspace.getName());
		
		List<DataStore> dataStores = service.getDataStores(workspace).get();
		assertNotNull(dataStores);
		assertTrue(dataStores.isEmpty());
		
		Map<String, String> connectionParameters = getConnectionParameters();
		service.postDataStore(workspace, new DataStore("testDataStore", connectionParameters)).get();
		
		dataStores = service.getDataStores(workspace).get();
		assertNotNull(dataStores);
		assertEquals(1, dataStores.size());
		
		DataStore dataStore = dataStores.get(0);
		assertNotNull(dataStore);
		assertEquals("testDataStore", dataStore.getName());
		connectionParameters = dataStore.getConnectionParameters();
		assertEquals("localhost", connectionParameters.get("host"));
		assertEquals("" + GeoServerTestHelper.PG_PORT, connectionParameters.get("port"));
		assertEquals("test", connectionParameters.get("database"));
		assertEquals("postgres", connectionParameters.get("user"));
		assertEquals("postgis", connectionParameters.get("dbtype"));
		assertEquals("public", connectionParameters.get("schema"));
		
		List<FeatureType> featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertTrue(featureTypes.isEmpty());
		
		service.postFeatureType(workspace, dataStore, new FeatureType("test", "test_table", "title", "abstract")).get();
		
		featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertEquals(1, featureTypes.size());
		
		FeatureType featureType = featureTypes.get(0);
		assertNotNull(featureType);
		
		assertEquals("test", featureType.getName());
		assertEquals("test_table", featureType.getNativeName());
		
		List<Attribute> attributes = featureType.getAttributes();
		assertNotNull(attributes);
		assertEquals(3, attributes.size());
		
		Attribute attribute = attributes.get(0);
		assertNotNull(attribute);
		assertEquals("id", attribute.getName());
		
		attribute = attributes.get(1);
		assertNotNull(attribute);
		assertEquals("test", attribute.getName());
		
		attribute = attributes.get(2);
		assertNotNull(attribute);
		assertEquals("the_geom", attribute.getName());
		
		Iterable<LayerGroup> layerGroups = service.getLayerGroups(workspace).get();
		assertNotNull(layerGroups);
		assertFalse(layerGroups.iterator().hasNext());
		
		LayerGroup layerGroup = new LayerGroup("group", "title", "abstract", Arrays.asList(new LayerRef("test", false)));
		service.postLayerGroup(workspace, layerGroup).get();
		
		layerGroups = service.getLayerGroups(workspace).get();
		assertNotNull(layerGroups);
		
		Iterator<LayerGroup> itr = layerGroups.iterator();
		assertTrue(itr.hasNext());
		layerGroup = itr.next();
		assertEquals("group", layerGroup.getName());
		
		List<LayerRef> layers = layerGroup.getLayers();
		assertEquals(1, layers.size());
		LayerRef layerRef = layers.get(0);		
		assertNotNull(layerRef);
		assertEquals("test", layerRef.getLayerName());
		assertEquals(false, layerRef.isGroup());
		
		assertFalse(itr.hasNext());
	}

	private Map<String, String> getConnectionParameters() {
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("host", "localhost");
		connectionParameters.put("port", "" + PgServer.DEFAULT_PORT);
		connectionParameters.put("database", "test");
		connectionParameters.put("user", "postgres");
		connectionParameters.put("passwd", "postgres");
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("schema", "public");
		return connectionParameters;
	}
	
	@Test
	public void testServiceSettings() throws Exception {
		
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		assertFalse(service.getServiceSettings(workspace, ServiceType.WMS).get().isPresent());
		
		ServiceSettings serviceSettings = new ServiceSettings("MyTitle", "MyAbstract", Arrays.asList("keyword0", "keyword1", "keyword2"));
		service.putServiceSettings(workspace, ServiceType.WMS, serviceSettings).get();
		
		Document capabilities = h.getCapabilities(workspace.getName(), ServiceType.WMS, "1.3.0");
		assertEquals("MyTitle", h.getText("//wms:Service/wms:Title", capabilities));
		assertEquals("MyAbstract", h.getText("//wms:Service/wms:Abstract", capabilities));
		assertEquals(Arrays.asList("keyword0", "keyword1", "keyword2"), h.getText(
			h.getNodeList("//wms:Service/wms:KeywordList/wms:Keyword", capabilities)));
		
		Optional<ServiceSettings> optionalServiceSettings = service.getServiceSettings(workspace, ServiceType.WMS).get();
		assertTrue(optionalServiceSettings.isPresent());
		
		serviceSettings = optionalServiceSettings.get();
		assertEquals("MyTitle", serviceSettings.getTitle());
		assertEquals("MyAbstract", serviceSettings.getAbstract());
		assertEquals(Arrays.asList("keyword0", "keyword1", "keyword2"), serviceSettings.getKeywords());
	}
	
	@Test
	public void testWorkspaceSettings() throws Exception {
		
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		WorkspaceSettings workspaceSettings = new WorkspaceSettings("MyContact", "MyOrganization", 
			"MyPosition", "MyAddressType", "MyAddress", "MyCity", "MyState", "MyZipcode", 
			"MyCountry", "MyTelephone", "MyFax", "MyEmail");
		
		service.putWorkspaceSettings(workspace, workspaceSettings).get();
		
		workspaceSettings = service.getWorkspaceSettings(workspace).get();
		assertEquals("MyContact", workspaceSettings.getContact()); 
		assertEquals("MyOrganization", workspaceSettings.getOrganization());
		assertEquals("MyPosition", workspaceSettings.getPosition());
		assertEquals("MyAddressType", workspaceSettings.getAddressType());
		assertEquals("MyAddress", workspaceSettings.getAddress());
		assertEquals("MyCity", workspaceSettings.getCity());
		assertEquals("MyState", workspaceSettings.getState());
		assertEquals("MyZipcode", workspaceSettings.getZipcode());
		assertEquals("MyCountry", workspaceSettings.getCountry());
		assertEquals("MyTelephone", workspaceSettings.getTelephone());
		assertEquals("MyFax", workspaceSettings.getFax());
		assertEquals("MyEmail", workspaceSettings.getEmail());
		
		service.deleteWorkspace(workspace).get();		
		assertTrue(service.getWorkspaces().get().isEmpty());
		
		service.close();
	}
	
	@Test
	public void testServiceSettingsEquals() throws Exception {
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		ServiceSettings serviceSettings = new ServiceSettings("MyTitle", null, null);
		service.putServiceSettings(workspace, ServiceType.WFS, serviceSettings).get();
		
		assertEquals(serviceSettings, service.getServiceSettings(workspace, ServiceType.WFS).get().get());
	}
	
	@Test
	public void testStyles() throws Exception {
		assertFalse(
			service.getStyles().get().stream()
				.map(style -> style.getName())			
				.collect(Collectors.toSet())
				.contains("green"));
		
		InputStream greenInputStream = getClass().getResourceAsStream("green.sld");
		assertNotNull(greenInputStream);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Style green = new Style("green", db.parse(greenInputStream));
		service.postStyle(green).get();
		
		Map<String, Document> styles = service.getStyles().get().stream()
			.collect(Collectors.toMap(
				style -> style.getName(), 
				style -> style.getSld()));
		
		Document sld = styles.get("green");
		assertNotNull(sld);
		
		assertEquals("#66FF66", h.getText("//sld:CssParameter", sld));
		
		h.getNodeList("//sld:CssParameter", sld).item(0).setTextContent("#00FF00");
		
		service.putStyle(new Style("green", sld)).get();
		
		assertEquals("#00FF00", h.getText("//sld:CssParameter", service.getStyle("green").get().get().getSld()));
	}
	
	@Test
	public void testLayer() throws Exception {
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		DataStore dataStore = new DataStore("dataStore", getConnectionParameters());
		service.postDataStore(workspace, dataStore).get();
		
		FeatureType featureType = new FeatureType("test", "test_table", "title", "abstract");
		service.postFeatureType(workspace, dataStore, featureType).get();
		
		assertNotEquals("green", service.getLayer(workspace, featureType).get().getDefaultStyle().getStyleName());
		
		InputStream greenInputStream = getClass().getResourceAsStream("green.sld");
		assertNotNull(greenInputStream);
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Document sld = db.parse(greenInputStream);
		service.postStyle(new Style("green", sld)).get();
		
		NodeList names = h.getNodeList("//sld:Name", sld);
		for(int i = 0; i < names.getLength(); i++) {
			names.item(i).setTextContent("red");
		}		
		h.getNodeList("//sld:CssParameter", sld).item(0).setTextContent("#FF0000");		
		service.postStyle(new Style("red", sld)).get();
		
		service.putLayer(workspace, new Layer("test", new StyleRef("green"), null)).get();
		Layer layer = service.getLayer(workspace, featureType).get();
		assertEquals("green", layer.getDefaultStyle().getStyleName());
		assertEquals(Collections.emptyList(), layer.getAdditionalStyles());
		
		service.putLayer(workspace, new Layer("test", new StyleRef("green"), Collections.singletonList(new StyleRef("red")))).get();
		layer = service.getLayer(workspace, featureType).get();
		assertEquals("green", layer.getDefaultStyle().getStyleName());
		assertEquals(Collections.singletonList(new StyleRef("red")), layer.getAdditionalStyles());
	}
}
