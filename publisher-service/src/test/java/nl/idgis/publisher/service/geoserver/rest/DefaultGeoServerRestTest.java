package nl.idgis.publisher.service.geoserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.service.geoserver.TestServers;
import nl.idgis.publisher.service.geoserver.rest.Attribute;
import nl.idgis.publisher.service.geoserver.rest.DataStore;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.FeatureType;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.utils.FutureUtils;

import org.h2.server.pg.PgServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorSystem;

public class DefaultGeoServerRestTest {
	
	TestServers testServers;
	
	FutureUtils f;
	
	@Before
	public void startServers() throws Exception {
		testServers = new TestServers();
		testServers.start();
	} 
	
	@Before
	public void async() throws Exception {
		ActorSystem actorSystem = ActorSystem.create();
		f = new FutureUtils(actorSystem.dispatcher());
	}
	
	@After
	public void stopServers() throws Exception {
		testServers.stop();
	}

	@Test
	public void doTest() throws Exception {
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + TestServers.PG_PORT + "/test", "postgres", "postgres");
		
		Statement stmt = connection.createStatement();
		stmt.execute("create schema \"public\"");
		stmt.execute("create table \"public\".\"test_table\"(\"id\" serial, \"test\" integer)");
		stmt.execute("select AddGeometryColumn ('public', 'test_table', 'the_geom', 4326, 'GEOMETRY', 2)");
		stmt.execute("create schema \"b0\"");
		stmt.execute("create table \"b0\".\"another_test_table\"(\"id\" serial, \"test\" integer)");
		
		stmt.close();
				
		connection.close();
		
		GeoServerRest service = new DefaultGeoServerRest("http://localhost:" + TestServers.JETTY_PORT + "/rest/", "admin", "geoserver");
		
		List<Workspace> workspaces = service.getWorkspaces().get();
		assertNotNull(workspaces);
		assertTrue(workspaces.isEmpty());
		
		service.addWorkspace(new Workspace("testWorkspace")).get();
		
		workspaces = service.getWorkspaces().get();
		assertNotNull(workspaces);
		assertEquals(1, workspaces.size());
		
		Workspace workspace = workspaces.get(0);
		assertNotNull(workspace);
		assertEquals("testWorkspace", workspace.getName());
		
		List<CompletableFuture<DataStore>> dataStores = service.getDataStores(workspace).get();
		assertNotNull(dataStores);
		assertTrue(dataStores.isEmpty());
		
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("host", "localhost");
		connectionParameters.put("port", "" + PgServer.DEFAULT_PORT);
		connectionParameters.put("database", "test");
		connectionParameters.put("user", "postgres");
		connectionParameters.put("passwd", "postgres");
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("schema", "public");
		service.addDataStore(workspace, new DataStore("testDataStore", connectionParameters)).get();
		
		dataStores = service.getDataStores(workspace).get();
		assertNotNull(dataStores);
		assertEquals(1, dataStores.size());
		
		DataStore dataStore = dataStores.get(0).get();
		assertNotNull(dataStore);
		assertEquals("testDataStore", dataStore.getName());
		connectionParameters = dataStore.getConnectionParameters();
		assertEquals("localhost", connectionParameters.get("host"));
		assertEquals("" + TestServers.PG_PORT, connectionParameters.get("port"));
		assertEquals("test", connectionParameters.get("database"));
		assertEquals("postgres", connectionParameters.get("user"));
		assertEquals("postgis", connectionParameters.get("dbtype"));
		assertEquals("public", connectionParameters.get("schema"));
		
		List<CompletableFuture<FeatureType>> featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertTrue(featureTypes.isEmpty());
		
		service.addFeatureType(workspace, dataStore, new FeatureType("test", "test_table", "title", "abstract")).get();
		
		featureTypes = service.getFeatureTypes(workspace, dataStore).get();
		assertNotNull(featureTypes);
		assertEquals(1, featureTypes.size());
		
		FeatureType featureType = featureTypes.get(0).get();
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
		
		Iterable<LayerGroup> layerGroups = service.getLayerGroups(workspace).thenCompose(f::sequence).get();
		assertNotNull(layerGroups);
		assertFalse(layerGroups.iterator().hasNext());
		
		LayerGroup layerGroup = new LayerGroup("group", "title", "abstract", Arrays.asList("test"));
		service.addLayerGroup(workspace, layerGroup).get();
		
		layerGroups = service.getLayerGroups(workspace).thenCompose(f::sequence).get();
		assertNotNull(layerGroups);
		
		Iterator<LayerGroup> itr = layerGroups.iterator();
		assertTrue(itr.hasNext());
		layerGroup = itr.next();
		assertEquals("group", layerGroup.getName());
		assertEquals(Arrays.asList("test"), layerGroup.getLayers());
		assertFalse(itr.hasNext());
		
		service.close();
	}
}
