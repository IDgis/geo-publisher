package nl.idgis.publisher.service.geoserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;
import akka.util.Timeout;
import nl.idgis.publisher.service.geoserver.GeoServerTestHelper;
import nl.idgis.publisher.service.raster.TestRaster;
import nl.idgis.publisher.service.style.TestStyle;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.Logging;
import nl.idgis.publisher.utils.XMLUtils;

public class DefaultGeoServerRestTest {
	
	static LoggingAdapter log = Logging.getLogger();
	
	static GeoServerTestHelper h;
	
	static GeoServerRest service;
	
	static FutureUtils f;
	
	@Before
	public void initDb() throws Exception {
		try(Connection connection = getConnection()) {		
			try(Statement stmt = connection.createStatement()) {
				stmt.execute("create table \"public\".\"test_table\"(\"id\" serial, \"test\" integer, \"dummy\" integer)");
				stmt.execute("select AddGeometryColumn ('public', 'test_table', 'the_geom', 4326, 'GEOMETRY', 2)");
				stmt.execute("create schema \"b0\"");
				stmt.execute("create table \"b0\".\"another_test_table\"(\"id\" serial, \"test\" integer)");
				stmt.execute("create table public.gt_pk_metadata("
						+ "table_schema text not null, "
						+ "table_name text not null, "
						+ "pk_column text not null, "
						+ "pk_column_idx integer, "
						+ "pk_policy text, "
						+ "pk_sequence text)");
			}
		}
	}
	
	@After
	public void cleanDb() throws Exception {
		try(Connection connection = getConnection()) {		
			try(Statement stmt = connection.createStatement()) {
				stmt.execute("drop table \"public\".\"test_table\"");
				stmt.execute("drop table \"public\".\"gt_pk_metadata\"");
				stmt.execute("drop schema \"b0\" cascade");
			}
		}
	}
	
	@BeforeClass
	public static void startServers() throws Exception {
		h = new GeoServerTestHelper();
		h.start();
		
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		ActorSystem actorSystem = ActorSystem.create("test", akkaConfig);
		f = new FutureUtils(actorSystem, Timeout.apply(30, TimeUnit.SECONDS));
		service = new DefaultGeoServerRest(f, log, "http://localhost:" + h.getGeoserverPort() + "/geoserver/", "admin", "geoserver");
	}

	private static Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:postgresql://" + h.getDbHost() + ":" + h.getDbPort() + "/test", "postgres", "postgres");
	}
	
	@After
	public void clean() throws Exception {
		h.clean(f, log);
	}
	
	@Test
	public void testFeatureTypeId() throws Exception {
		final int numberOfFeatures = 10;
		
		// insert some test features
		try(
			Connection c = getConnection(); 
			PreparedStatement stmt = c.prepareStatement("insert into \"public\".\"test_table\"(\"test\") values(?)")) {
			 
			for(int i = 0; i < numberOfFeatures; i++) {
				stmt.setInt(1, i);
				assertEquals(1, stmt.executeUpdate());
			}
		}
		
		// configure feature type
		Workspace workspace = new Workspace("testWorkspace");
		service.postWorkspace(workspace).get();
		
		DataStore dataStore = new DataStore("testDataStore", getConnectionParameters());
		service.postDataStore(workspace, dataStore).get();
		
		service.postFeatureType(workspace, dataStore, new FeatureType(
			"test", "test_table", "title", "abstract", 
				Arrays.asList("keyword0", "keyword1"),
				Collections.emptyList(),
				Arrays.asList(
					new Attribute("id"),
					new Attribute("test"),
					new Attribute("the_geom")))).get();
		
		Map<String, String> ns = new HashMap<>();
		ns.put("wfs", "http://www.opengis.net/wfs");
		ns.put("gml", "http://www.opengis.net/gml");
		ns.put("testWorkspace", "http://testWorkspace");
		
		// assert that ids are random
		Document featureCollection = h.getFeature("testWorkspace", "test");
		
		List<String[]> ids = XMLUtils.xpath(featureCollection, ns).map(
				"/wfs:FeatureCollection//testWorkspace:test[@gml:id]", n -> new String[] {
					n.string("testWorkspace:id/text()").get(),
					n.string("@gml:id").get() });
			
		assertEquals(numberOfFeatures, ids.size());
		
		for(String[] id : ids) {
			assertTrue(id[1].startsWith("test."));
			assertNotEquals("test." + id[0], id[1]);
		}
		
		// configure id column
		try(
			Connection c = getConnection();
			Statement stmt = c.createStatement()) {
			
			stmt.executeUpdate("insert into public.gt_pk_metadata("
					+ "table_schema,"
					+ "table_name,"
					+ "pk_column,"
					+ "pk_column_idx,"
					+ "pk_policy) values("
					+ "'public',"
					+ "'test_table',"
					+ "'id',"
					+ "0,"
					+ "'assigned')");
		}
		
		// empty cache
		service.reset().get();		
		
		// assert that ids are based on id column
		featureCollection = h.getFeature("testWorkspace", "test");
		
		ids = XMLUtils.xpath(featureCollection, ns).map(
			"/wfs:FeatureCollection//testWorkspace:test[@gml:id]", n -> new String[] {
				n.string("testWorkspace:id/text()").get(),
				n.string("@gml:id").get() });
		
		assertEquals(numberOfFeatures, ids.size());
		
		for(String[] id : ids) {
			assertEquals("test." + id[0], id[1]);
		}
	}
	
	@Test
	public void testFeatureTypeDropAttribute() throws Exception {
		Workspace workspace = new Workspace("testWorkspace");
		service.postWorkspace(workspace).get();
		
		DataStore dataStore = new DataStore("testDataStore", getConnectionParameters());
		service.postDataStore(workspace, dataStore).get();
		
		service.postFeatureType(workspace, dataStore, new FeatureType(
			"test", "test_table", "title", "abstract", 
				Arrays.asList("keyword0", "keyword1"),
				Collections.emptyList(),
				Arrays.asList(
					new Attribute("id"),
					new Attribute("test"),
					new Attribute("the_geom")))).get();
		
		// remove column that's in use by configured FeatureType
		try(Connection connection = getConnection()) {		
			try(Statement stmt = connection.createStatement()) {
				stmt.execute("alter table \"public\".\"test_table\" drop column \"test\"");
			}
		}
		
		// FeatureType attribute information is being cached
		List<FeatureType> featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertEquals(1, featureTypes.size());
		
		FeatureType featureType = featureTypes.get(0);
		assertNotNull(featureType);
		
		assertEquals(
			Arrays.asList("id", "test", "the_geom"),
			featureType.getAttributes().stream()
				.map(Attribute::getName)
				.collect(Collectors.toList()));
		
		// empty cache
		service.reset().get();
		
		// FeatureType attribute information is lost
		featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertEquals(1, featureTypes.size());
		
		featureType = featureTypes.get(0);
		assertNotNull(featureType);
		assertTrue(featureType.getAttributes().isEmpty());
		
		// remove missing attribute from configuration
		service.putFeatureType(workspace, dataStore, new FeatureType(
			"test", "test_table", "title", "abstract", 
				Arrays.asList("keyword0", "keyword1"),
				Collections.emptyList(),
				Arrays.asList(
					new Attribute("id"),
					new Attribute("the_geom")))).get();
		
		// attribute information should be available again
		featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertEquals(1, featureTypes.size());
		
		featureType = featureTypes.get(0);
		assertNotNull(featureType);
		
		assertEquals(
			Arrays.asList("id", "the_geom"),
			featureType.getAttributes().stream()
				.map(Attribute::getName)
				.collect(Collectors.toList()));
	}

	@Test
	public void testFeatureType() throws Exception {
		
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
		assertEquals(h.getDatastoreDbHost(), connectionParameters.get("host"));
		assertEquals("5432", connectionParameters.get("port"));
		assertEquals("test", connectionParameters.get("database"));
		assertEquals("postgres", connectionParameters.get("user"));
		assertEquals("postgis", connectionParameters.get("dbtype"));
		assertEquals("public", connectionParameters.get("schema"));
		
		List<FeatureType> featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertTrue(featureTypes.isEmpty());
		
		service.postFeatureType(workspace, dataStore, new FeatureType(
			"test", "test_table", "title", "abstract", 
				Arrays.asList("keyword0", "keyword1"),
				Arrays.asList(
					new MetadataLink("text/plain", "ISO19115:2003", "content")),
				Arrays.asList(
					new Attribute("id"),
					new Attribute("test"),
					new Attribute("the_geom")))).get();
		
		featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertEquals(1, featureTypes.size());
		
		FeatureType featureType = featureTypes.get(0);
		assertNotNull(featureType);
		
		assertEquals("test", featureType.getName());
		assertEquals("test_table", featureType.getNativeName());
		
		List<String> keywords = featureType.getKeywords();
		assertNotNull(keywords);
		assertTrue(keywords.contains("keyword0"));
		assertTrue(keywords.contains("keyword1"));
		
		List<MetadataLink> metadataLinks = featureType.getMetadataLinks();
		assertNotNull(metadataLinks);
		assertEquals(1, metadataLinks.size());
		
		MetadataLink metadataLink = metadataLinks.get(0);
		assertNotNull(metadataLink);		
		assertEquals("text/plain", metadataLink.getType());
		assertEquals("ISO19115:2003", metadataLink.getMetadataType());
		assertEquals("content", metadataLink.getContent());
		
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
		
		Optional<TiledLayer> tiledLayer = service.getTiledLayer(workspace, featureType).get();
		assertTrue(tiledLayer.isPresent());
		
		service.deleteTiledLayer(workspace, featureType).get();		
		//assertFalse(service.getTiledLayer(workspace, featureType).get().isPresent());
	}
	
	@Test
	public void testLayerGroup() throws Exception {
		
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		Iterable<LayerGroup> layerGroups = service.getLayerGroups(workspace).get();
		assertNotNull(layerGroups);
		assertFalse(layerGroups.iterator().hasNext());
		 
		DataStore dataStore = new DataStore("testDataStore", getConnectionParameters());
		service.postDataStore(workspace, dataStore).get();
		
		service.postFeatureType(workspace, dataStore, new FeatureType(
			"test", "test_table", "title", "abstract", 
				Arrays.asList("keyword0", "keyword1"),
				Collections.emptyList(),
				Arrays.asList(
					new Attribute("id"), 
					new Attribute("test"), 
					new Attribute("the_geom")))).get();
		
		LayerGroup layerGroup = new LayerGroup("group", "title", "abstract", Arrays.asList(new LayerRef("test")));
		service.postLayerGroup(workspace, layerGroup).get();
		
		layerGroups = service.getLayerGroups(workspace).get();
		assertNotNull(layerGroups);
		
		Iterator<LayerGroup> itr = layerGroups.iterator();
		assertTrue(itr.hasNext());
		layerGroup = itr.next();
		assertEquals("group", layerGroup.getName());
		
		List<PublishedRef> layers = layerGroup.getLayers();
		assertEquals(1, layers.size());
		PublishedRef layerRef = layers.get(0);
		assertNotNull(layerRef);
		assertEquals("workspace:test", layerRef.getLayerName());
		assertEquals(false, layerRef.isGroup());
		
		assertFalse(itr.hasNext());
		
		Optional<TiledLayer> tiledLayer = service.getTiledLayer(workspace, layerGroup).get();
		assertTrue(tiledLayer.isPresent());
		
		service.deleteTiledLayer(workspace, layerGroup).get();		
		//assertFalse(service.getTiledLayer(workspace, layerGroup).get().isPresent());
	}
	
	@Test
	public void testLayerEmptyGroup () throws Exception {
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		DataStore dataStore = new DataStore("testDataStore", getConnectionParameters());
		service.postDataStore(workspace, dataStore).get();
		
		LayerGroup layerGroup = new LayerGroup("group", "title", "abstract", Arrays.asList ());
		try {
			service.postLayerGroup(workspace, layerGroup).get();
		} catch (ExecutionException e) {
			assertNotNull (e.getCause ());
			assertTrue (e.getCause () instanceof GeoServerException);
			
			final GeoServerException geoServerException = (GeoServerException) e.getCause ();
			
			assertEquals (Integer.valueOf (400), geoServerException.getHttpResponse ());
			
			return; 
		}
		
		fail ("Expected: GeoServerException");
	}

	private Map<String, String> getConnectionParameters() {
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("host", h.getDatastoreDbHost());
		connectionParameters.put("port", "5432");
		connectionParameters.put("database", "test");
		connectionParameters.put("user", "postgres");
		connectionParameters.put("passwd", "postgres");
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("schema", "public");
		connectionParameters.put("Expose primary keys", "true");
		return connectionParameters;
	}
	
	@Ignore
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
	
	@Ignore
	@Test
	public void testWorkspaceSettings() throws Exception {
		
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		ServiceSettings serviceSettings = new ServiceSettings("MyTitle", "MyAbstract", Arrays.asList("keyword0", "keyword1", "keyword2"));
		service.putServiceSettings(workspace, ServiceType.WMS, serviceSettings).get();
		
		Document capabilities = h.getCapabilities(workspace.getName(), ServiceType.WMS, "1.3.0");
		assertEquals("MyTitle", h.getText("//wms:Service/wms:Title", capabilities));
		
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
		
		capabilities = h.getCapabilities(workspace.getName(), ServiceType.WMS, "1.3.0");
		
		assertEquals("MyTelephone", h.getText("//wms:Service/wms:ContactInformation/wms:ContactVoiceTelephone", capabilities));
		
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
	
	@Ignore
	@Test
	public void testStyles() throws Exception {
		assertFalse(
			service.getStyleNames().get().stream()
				.collect(Collectors.toSet())
				.contains("green"));
		
		Style green = new Style("green", TestStyle.getGreenSld());
		service.postStyle(green).get();
		
		Map<String, Document> styles = new HashMap<>();
		for(String styleName : service.getStyleNames().get()) {
			styles.put(styleName, service.getStyle(styleName).get().get().getSld());
		}
		
		Document sld = styles.get("green");
		assertNotNull(sld);
		
		assertEquals("#66FF66", h.getText("//sld:CssParameter", sld));
		
		h.getNodeList("//sld:CssParameter", sld).item(0).setTextContent("#00FF00");
		
		service.putStyle(new Style("green", sld)).get();
		
		assertEquals("#00FF00", h.getText("//sld:CssParameter", service.getStyle("green").get().get().getSld()));
		
		h.getNodeList("//sld:CssParameter", sld).item(0).setTextContent("#FF0000");
		service.postStyle(new Style("red", sld)).get();
		
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		DataStore dataStore = new DataStore("dataStore", getConnectionParameters());
		service.postDataStore(workspace, dataStore).get();
		
		FeatureType featureType = new FeatureType("test", "test_table", "test", "test", 
			Arrays.asList("test"),
			Collections.emptyList(),
			Arrays.asList(
				new Attribute("id"), 
				new Attribute("test"), 
				new Attribute("the_geom")));		
		service.postFeatureType(workspace, dataStore, featureType).get();
		
		service.putLayer(workspace, new Layer("test", new StyleRef("green"), Arrays.asList(new StyleRef("red")), false)).get();
		
		Layer layer = service.getLayer(workspace, featureType).get();
		assertEquals("green", layer.getDefaultStyle().getStyleName());
		
		List<StyleRef> additionalStyles = layer.getAdditionalStyles();
		assertNotNull(additionalStyles);
		assertEquals(1, additionalStyles.size());
		assertEquals("red", additionalStyles.get(0).getStyleName());
		
		service.postLayerGroup(workspace, new LayerGroup("name", "title", "abstract", Arrays.asList(new LayerRef("test", "red")))).get();
		
		LayerGroup layerGroup = service.getLayerGroups(workspace).get().get(0);
		assertNotNull(layerGroup);
		
		List<PublishedRef> layers = layerGroup.getLayers();
		assertNotNull(layers);
		assertEquals(1, layers.size());
		
		PublishedRef publishedRef = layers.get(0);
		assertNotNull(publishedRef);
		assertFalse(publishedRef.isGroup());
		
		LayerRef layerRef = publishedRef.asLayerRef();
		assertEquals("workspace:test", layerRef.getLayerName());
		
		Optional<String> styleName = layerRef.getStyleName();
		assertTrue(styleName.isPresent());
		assertEquals("red", styleName.get());
	}
	
	@Test
	public void testLayer() throws Exception {
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		DataStore dataStore = new DataStore("dataStore", getConnectionParameters());
		service.postDataStore(workspace, dataStore).get();
		
		FeatureType featureType = new FeatureType(
			"test", "test_table", "title", "abstract", 
				Arrays.asList("keyword0", "keyword1"),
				Collections.emptyList(),
				Arrays.asList(
						new Attribute("id"), 
						new Attribute("test"), 
						new Attribute("the_geom")));
		service.postFeatureType(workspace, dataStore, featureType).get();
		
		assertNotEquals("green", service.getLayer(workspace, featureType).get().getDefaultStyle().getStyleName());
		
		Document sld = TestStyle.getGreenSld();
		service.postStyle(new Style("green", sld)).get();
		
		NodeList names = h.getNodeList("//sld:Name", sld);
		for(int i = 0; i < names.getLength(); i++) {
			names.item(i).setTextContent("red");
		}		
		h.getNodeList("//sld:CssParameter", sld).item(0).setTextContent("#FF0000");		
		service.postStyle(new Style("red", sld)).get();
		
		service.putLayer(workspace, new Layer("test", new StyleRef("green"), null, false)).get();
		Layer layer = service.getLayer(workspace, featureType).get();
		assertEquals("green", layer.getDefaultStyle().getStyleName());
		assertEquals(Collections.emptyList(), layer.getAdditionalStyles());
		
		service.putLayer(workspace, new Layer("test", new StyleRef("green"), Collections.singletonList(new StyleRef("red")), false)).get();
		layer = service.getLayer(workspace, featureType).get();
		assertEquals("green", layer.getDefaultStyle().getStyleName());
		assertEquals(Collections.singletonList(new StyleRef("red")), layer.getAdditionalStyles());
		
		// remove default tiled layer
		service.deleteTiledLayer(workspace, featureType).get(); 
		// new tiled layer (in GWC put and post are swapped)
		service.putTiledLayer(workspace, featureType.getName(), new TiledLayer(Arrays.asList("image/png"), 
			Arrays.asList(new GridSubset(
				"urn:ogc:def:wkss:OGC:1.0:NLDEPSG28992Scale",
				Optional.of(1),
				Optional.of(13))), 
			4, 4, 0, 0, 0)).get(); 
		
		Optional<TiledLayer> tiledLayer = service.getTiledLayer(workspace, featureType).get();
		assertTrue(tiledLayer.isPresent());
		
		assertEquals(Arrays.asList("image/png"), tiledLayer.get().getMimeFormats());
		assertEquals(Arrays.asList("test"), service.getTiledLayerNames(workspace).get());
		
		List<GridSubset> gridSubsets = tiledLayer.get().getGridSubsets();
		assertEquals(1, gridSubsets.size());		
		
		GridSubset gridSubset = gridSubsets.get(0);
		assertEquals("urn:ogc:def:wkss:OGC:1.0:NLDEPSG28992Scale", gridSubset.getGridSetName());
		assertEquals(Optional.of(1), gridSubset.getMinCachedLevel());
		assertEquals(Optional.of(13), gridSubset.getMaxCachedLevel());
		
		// update tiled layer
		service.postTiledLayer(workspace, featureType.getName(), new TiledLayer(Arrays.asList("image/jpg"),
			Arrays.asList(new GridSubset(
				"urn:ogc:def:wkss:OGC:1.0:NLDEPSG28992Scale",
				Optional.empty(),
				Optional.of(11))), 
				4, 4, 0, 0, 0)).get();
		tiledLayer = service.getTiledLayer(workspace, featureType).get();
		assertEquals(Arrays.asList("image/jpg"), tiledLayer.get().getMimeFormats());
		
		gridSubsets = tiledLayer.get().getGridSubsets();
		assertEquals(1, gridSubsets.size());
		
		gridSubset = gridSubsets.get(0);
		assertEquals("urn:ogc:def:wkss:OGC:1.0:NLDEPSG28992Scale", gridSubset.getGridSetName());
		assertFalse(gridSubset.getMinCachedLevel().isPresent());
		assertEquals(Optional.of(11), gridSubset.getMaxCachedLevel());
		
		Workspace anotherWorkspace = new Workspace("anotherWorkspace");
		service.postWorkspace(anotherWorkspace).get();
		
		DataStore anotherDataStore = new DataStore("dataStore", getConnectionParameters());
		service.postDataStore(anotherWorkspace, dataStore).get();
		
		FeatureType anotherFeatureType = new FeatureType(
			"anotherTest", "test_table", "title", "abstract",			
			Arrays.asList("keyword0", "keyword1"),
			Collections.emptyList(),
			Arrays.asList(
				new Attribute("id"), 
				new Attribute("test"), 
				new Attribute("the_geom")));
		service.postFeatureType(anotherWorkspace, anotherDataStore, anotherFeatureType).get();
				
		assertEquals(Arrays.asList("test"), service.getTiledLayerNames(workspace).get());
		assertEquals(Arrays.asList("anotherTest"), service.getTiledLayerNames(anotherWorkspace).get());
	}
	
	@Test
	public void testCoverage() throws Exception {
		URL testRasterUrl = TestRaster.getRasterUrl();
		assertEquals("file", testRasterUrl.getProtocol());
		
		File testRasterFile = new File(testRasterUrl.toURI ().getPath ());
		assertTrue(testRasterFile.exists());
		
		Workspace workspace = new Workspace("workspace");
		service.postWorkspace(workspace).get();
		
		CoverageStore coverageStore = new CoverageStore("test", TestRaster.getRasterUrlGeoServerContainer());
		service.postCoverageStore(workspace, coverageStore).get();
	
		String nativeName = testRasterFile.getName().split("\\.")[0];
		Coverage coverage = new Coverage("test", nativeName, "title", 
			"abstract", Arrays.asList("keyword0", "keyword1"), 
				Arrays.asList(new MetadataLink("text/plain", "ISO19115:2003", "content")));
		service.postCoverage(workspace, coverageStore, coverage).get();
		
		Layer layer = service.getLayer(workspace, coverage).get();
		assertEquals("raster", layer.getDefaultStyle().getStyleName());
		
		Coverage modifiedCoveraged = new Coverage(coverage.getName(), coverage.getNativeName(), 
			"modified title", coverage.getAbstract(), coverage.getKeywords(), coverage.getMetadataLinks());
		service.putCoverage(workspace, coverageStore, modifiedCoveraged).get();
		
		List<Coverage> coverages = service.getCoverages(workspace, coverageStore).get();
		assertEquals(1, coverages.size());
		
		Coverage retrievedCoverage = coverages.get(0);
		assertNotNull(retrievedCoverage);
		
		List<MetadataLink> metadataLinks = retrievedCoverage.getMetadataLinks();
		assertNotNull(metadataLinks);
		assertEquals(1, metadataLinks.size());
		
		MetadataLink metadataLink = metadataLinks.get(0);
		assertNotNull(metadataLink);		
		assertEquals("text/plain", metadataLink.getType());
		assertEquals("ISO19115:2003", metadataLink.getMetadataType());
		assertEquals("content", metadataLink.getContent());
		
		assertEquals("test", retrievedCoverage.getName());
		assertEquals(nativeName, retrievedCoverage.getNativeName());
		assertEquals("modified title", retrievedCoverage.getTitle());
		
		Map<CoverageStore, List<Coverage>> allCoverages = service.getCoverages(workspace).get();
		assertEquals(1, allCoverages.size());
		allCoverages.entrySet().stream().forEach(entry -> {
			CoverageStore retrievedCoverageStore = entry.getKey();
			
			assertEquals("test", retrievedCoverageStore.getName());
			assertEquals(TestRaster.getRasterUrlGeoServerContainer(), retrievedCoverageStore.getUrl());
			
			List<Coverage> retrievedCoverages = entry.getValue();
			assertEquals(1, retrievedCoverages.size());
			assertEquals("test", retrievedCoverages.get(0).getName());
		});
		
		service.deleteCoverageStore(workspace, coverageStore).get();
		
		allCoverages = service.getCoverages(workspace).get();
		assertTrue(allCoverages.isEmpty());
	}
	
	@Test
	public void testDeleteWorkspace() throws Exception {
		assertTrue(service.getWorkspaces().get().isEmpty());
		
		// TODO: remove dot from service names
		//String workspaceName = "it.geosolutions";
		String workspaceName = "geosolutions";
		
		Workspace workspace = new Workspace(workspaceName);
		service.postWorkspace(workspace).get();		
		
		List<Workspace> workspaces = service.getWorkspaces().get();
		assertFalse(workspaces.isEmpty());
		assertEquals(workspaceName, workspaces.get(0).getName());
		
		service.deleteWorkspace(workspace).get();		
		assertTrue(service.getWorkspaces().get().isEmpty());
	}
	
	@Test
	public void testManyWorkspaces() throws Exception {
		GeoServerRest rest = h.rest(f, log);
		
		assertTrue(rest.getWorkspaces().get().isEmpty());
		
		int workspaceCount = 10;
		
		for(int i = 0; i < workspaceCount; i++) {
			Workspace workspace = new Workspace("workspace" + i);
			rest.postWorkspace(workspace).get();
			
			Map<String, String> connectionParameters = new HashMap<>();									
			connectionParameters.put("dbtype", "postgis");
			connectionParameters.put("jndiReferenceName", "java:comp/env/jdbc/db");
			connectionParameters.put("schema", "public");
			DataStore dataStore = new DataStore("dataStore" + i, connectionParameters);
			rest.postDataStore(workspace, dataStore).get();
			
			FeatureType featureType = new FeatureType(
				"test", "test_table", "title", "abstract", 
					Arrays.asList("keyword0", "keyword1"),
					Collections.emptyList(),
					Arrays.asList(
							new Attribute("id"), 
							new Attribute("test"),
							new Attribute("the_geom")));
			service.postFeatureType(workspace, dataStore, featureType).get();
		}
		
		List<Workspace> workspaces = rest.getWorkspaces().get();
		assertEquals(workspaceCount, workspaces.size());
		
		for(Workspace workspace : workspaces) {
			List<DataStore> dataStores = rest.getDataStores(workspace).get();
			assertEquals(1, dataStores.size());
			
			List<FeatureType> featureTypes = rest.getFeatureTypes(workspace, dataStores.get(0)).get();
			assertEquals(1, featureTypes.size());
		}
		
		rest.close();
		
		// TODO: assert number of db connections == 1
	}
}
