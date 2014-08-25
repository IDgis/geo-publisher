package nl.idgis.publisher.service.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.service.rest.Attribute;
import nl.idgis.publisher.service.rest.DataStore;
import nl.idgis.publisher.service.rest.FeatureType;
import nl.idgis.publisher.service.rest.ServiceRest;
import nl.idgis.publisher.service.rest.Workspace;
import nl.idgis.publisher.utils.FileUtils;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.h2.server.pg.PgServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ServiceRestTest {
	
	private static final int JETTY_PORT = 7000;
	private static final int PG_PORT = PgServer.DEFAULT_PORT;
	
	Thread pgListenThread;
	PgServer pgServer;
	Server jettyServer;
	
	@Before
	public void startServers() throws Exception {
		pgServer = new PgServer();
		
		File baseDir = new File("target/geoserver-database");
		
		if(baseDir.exists()) {
			FileUtils.delete(baseDir);
		}
		
		pgServer.init("-pgPort", "" + PG_PORT, "-baseDir", baseDir.getAbsolutePath());
		
		pgServer.start();
		
		pgListenThread = new Thread() {
			
			@Override
			public void run() {
				pgServer.listen();
			}
		};
		
		pgListenThread.start();		
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + PgServer.DEFAULT_PORT + "/test", "postgres", "postgres");
		
		Statement stmt = connection.createStatement();		
		stmt.execute("create table \"test_table\"(\"id\" serial, \"test\" integer)");
		
		stmt.close();
				
		connection.close();
		
		File dataDir = new File("target/geoserver-data");
		
		if(dataDir.exists()) {
			FileUtils.delete(dataDir);
		}
		
		dataDir.mkdir();
		
		String geoserverDataDir = dataDir.getAbsolutePath();
		System.setProperty("GEOSERVER_DATA_DIR", geoserverDataDir);
		
		jettyServer = new Server(JETTY_PORT);
		WebAppContext context = new WebAppContext();
		File webXml = new File("target/geoserver/WEB-INF/web.xml");
		context.setDescriptor(webXml.getAbsolutePath());
		context.setResourceBase("target/geoserver");
		context.setContextPath("/");
		context.setParentLoaderPriority(false);
		jettyServer.setHandler(context);
		jettyServer.start();
	}
	
	@After
	public void stopServers() throws Exception {
		jettyServer.stop();
		pgServer.stop();
		
		pgListenThread.interrupt();
		pgListenThread.join();
	}

	@Test
	public void testRest() throws Exception {
		
		final ServiceRest service = new ServiceRest("http://localhost:" + JETTY_PORT + "/rest/", "admin", "geoserver");
		
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
		assertEquals("" + PG_PORT, connectionParameters.get("port"));
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
