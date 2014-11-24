package nl.idgis.publisher.service.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.service.TestServers;
import nl.idgis.publisher.service.rest.Attribute;
import nl.idgis.publisher.service.rest.DataStore;
import nl.idgis.publisher.service.rest.FeatureType;
import nl.idgis.publisher.service.rest.ServiceRest;
import nl.idgis.publisher.service.rest.Workspace;

import org.h2.server.pg.PgServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServiceRestTest {
	
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

	@Test
	public void testRest() throws Exception {
		
		final ServiceRest service = new ServiceRest("http://localhost:" + TestServers.JETTY_PORT + "/rest/", "admin", "geoserver");
		
		List<Workspace> workspaces = service.getWorkspaces();
		assertNotNull(workspaces);
		assertTrue(workspaces.isEmpty());
		
		assertTrue(service.addWorkspace(new Workspace("testWorkspace")));
		
		workspaces = service.getWorkspaces();
		assertNotNull(workspaces);
		assertEquals(1, workspaces.size());
		
		Workspace workspace = workspaces.get(0);
		assertNotNull(workspace);
		assertEquals("testWorkspace", workspace.getName());
		
		List<DataStore> dataStores = service.getDataStores(workspace);
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
		assertTrue(service.addDataStore(workspace, new DataStore("testDataStore", connectionParameters)));
		
		dataStores = service.getDataStores(workspace);
		assertNotNull(dataStores);
		assertEquals(1, dataStores.size());
		
		DataStore dataStore = dataStores.get(0);
		assertNotNull(dataStore);
		assertEquals("testDataStore", dataStore.getName());
		connectionParameters = dataStore.getConnectionParameters();
		assertEquals("localhost", connectionParameters.get("host"));
		assertEquals("" + TestServers.PG_PORT, connectionParameters.get("port"));
		assertEquals("test", connectionParameters.get("database"));
		assertEquals("postgres", connectionParameters.get("user"));
		assertEquals("postgis", connectionParameters.get("dbtype"));
		assertEquals("public", connectionParameters.get("schema"));
		
		List<FeatureType> featureTypes = service.getFeatureTypes(workspace, dataStore);
		assertNotNull(featureTypes);
		assertTrue(featureTypes.isEmpty());
		
		assertTrue(service.addFeatureType(workspace, dataStore, new FeatureType("test", "test_table")));
		
		featureTypes = service.getFeatureTypes(workspace, dataStore);
		assertNotNull(featureTypes);
		assertEquals(1, featureTypes.size());
		
		FeatureType featureType = featureTypes.get(0);
		assertNotNull(featureType);
		
		assertEquals("test", featureType.getName());
		assertEquals("test_table", featureType.getNativeName());
		
		List<Attribute> attributes = featureType.getAttributes();
		assertNotNull(attributes);
		assertEquals(2, attributes.size());
		
		Attribute attribute = attributes.get(0);
		assertNotNull(attribute);
		assertEquals("id", attribute.getName());
		
		attribute = attributes.get(1);
		assertNotNull(attribute);
		assertEquals("test", attribute.getName());
	}
}
