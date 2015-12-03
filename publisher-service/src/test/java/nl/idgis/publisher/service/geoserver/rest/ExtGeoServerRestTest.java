package nl.idgis.publisher.service.geoserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;
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

import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.Logging;
import nl.idgis.publisher.utils.XMLUtils;
import nl.idgis.publisher.utils.XMLUtils.XPathHelper;

public class ExtGeoServerRestTest {
	
	private final String host = "192.168.129.100";
	
	private ActorSystem actorSystem;
	
	private FutureUtils f;
	
	private LoggingAdapter log;
	
	private GeoServerRest rest;
	
	private Connection getConnection(String database) throws Exception {
		Properties props = new Properties();
		props.setProperty("user", "postgres");
		props.setProperty("password", "postgres");
		props.setProperty("socketTimeout", "2");
		props.setProperty("ApplicationName", "test"); 
		
		return DriverManager.getConnection("jdbc:postgresql://" + host + ":5432/" + database, props);
	}
	
	@Before
	public void init() throws Exception {
		actorSystem = ActorSystem.create();
		f = new FutureUtils(actorSystem);
		log = Logging.getLogger();
		
		rest = new DefaultGeoServerRest(f, log, "http://" + host + ":8080/geoserver/", "admin", "geoserver");
		
		try(Connection connection = getConnection("postgres");) {
			try(Statement stmt = connection.createStatement();) {
				stmt.execute("create database test");
			}
		}
	}
	
	interface ThrowingProcedure<T> {
		
		T apply() throws Exception;
	}
	
	public <T> T rethrow(ThrowingProcedure<T> procedure) {
		try {
			return procedure.apply();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@After
	public void clean() throws Exception {
		rest.getWorkspace("test").get().ifPresent(workspace -> {
			try {
				rest.deleteWorkspace(workspace).get();
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		rest.getStyleNames().get().forEach(styleName -> {
			try {				
				rest.deleteStyle(styleName);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		rest.close();
		
		try(Connection connection = getConnection("postgres");) {
			try(Statement stmt = connection.createStatement();) {
				System.out.println("locks:");
				try(ResultSet rs = stmt.executeQuery(
						"select l.pid, sa.query, sa.application_name, l.mode, l.granted " +
						"from pg_catalog.pg_locks l " +
						"join pg_catalog.pg_stat_activity sa on sa.pid = l.pid " +
						"where l.pid <>  pg_backend_pid() "
					);) {
					
					ResultSetMetaData md = rs.getMetaData();
					while(rs.next()) {
						System.out.println(
							IntStream.rangeClosed(1, md.getColumnCount())
								.mapToObj(i -> rethrow( () -> rs.getString(i)))
								.map(str -> str == null || str.trim().length() == 0 ? "null" : str)
								.collect(Collectors.joining(" ")));
					}
				}
				
				try(ResultSet rs = stmt.executeQuery("select count(pg_terminate_backend(pid)) from pg_stat_activity where datname = 'test'");) {
					assertTrue(rs.next());
					System.out.println("connections terminated: " + rs.getInt(1));
				}
				stmt.execute("drop database test");
			}
		}
	}
	
	@Test
	public void testFeatureTypes() throws Exception {
		final int featureCount = 10;
		
		try(Connection connection = getConnection("test");) {
			try(Statement stmt = connection.createStatement();) {
				stmt.execute("create extension postgis");
				stmt.execute("create table test(id serial, name text, the_geom geometry(POINT, 28992))");
				
				try(PreparedStatement pstmt = connection.prepareStatement("insert into test(name, the_geom) select ?, st_setsrid(st_point(?, ?), 28992)");) {
					for(int i = 0; i < featureCount; i++) {
						pstmt.setString(1, "name" + i);
						pstmt.setInt(2, i);
						pstmt.setInt(3, i * i);
						pstmt.addBatch();
					}
					
					pstmt.executeBatch();
				}
				
				try(ResultSet rs = stmt.executeQuery("select count(*) from test");) {
					assertTrue(rs.next());
					assertEquals(featureCount, rs.getInt(1));
				}
				
				stmt.execute("analyze test");
				
				try(ResultSet rs = stmt.executeQuery("select ST_Estimated_Extent('test', 'the_geom')");) {
					assertTrue(rs.next());
				}
			}
		}
		
		Workspace workspace = new Workspace("test");
		rest.postWorkspace(workspace).get();
		
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("schema", "public");
		
		/*connectionParameters.put("host", "localhost");
		connectionParameters.put("port", "5432");
		connectionParameters.put("database", "test");
		connectionParameters.put("user", "postgres");
		connectionParameters.put("passwd", "postgres");*/
		
		connectionParameters.put("jndiReferenceName", "java:comp/env/jdbc/db");
		
		DataStore dataStore = new DataStore("test", connectionParameters);
		rest.postDataStore(workspace, dataStore).get();
		
		FeatureType featureType = new FeatureType(
			"test", // name 
			"test", // nativeName
			"testTitle",
			"testAbstract",
			Collections.emptyList(), // keywords 
			Arrays.asList(
				new Attribute("id"),
				new Attribute("name"),
				new Attribute("the_geom")
			));
		
		rest.postFeatureType(workspace, dataStore, featureType).get();
		
		Map<String, String> ns = new HashMap<>();
		ns.put("wfs", "http://www.opengis.net/wfs");
		ns.put("gml", "http://www.opengis.net/gml");
		ns.put("test", "http://test");
		
		assertEquals(
			Arrays.asList("test:test"),
			getDocument(ns, "http://" + host + ":8080/geoserver/wfs?request=GetCapabilities&service=WFS&version=1.1.0")		
				.strings("wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType/wfs:Name"));
		
		int noOfRequests = 20;
		for(int i = 0; i < noOfRequests; i++) {
			assertEquals(
				featureCount,
				getDocument(ns, "http://" + host + ":8080/geoserver/wfs?request=GetFeature&service=WFS&version=1.1.0&typeName=test")
					.nodes("wfs:FeatureCollection/gml:featureMembers/test:test").size());
		}
		
		try(Connection connection = getConnection("test");)  {
			try(Statement stmt = connection.createStatement();) {
				stmt.execute("alter table test drop column name");
			}
		}
		
		Iterator<FeatureType> itr = rest.getFeatureTypes(workspace, dataStore).get().iterator();
		assertTrue(itr.hasNext());
		
		featureType = itr.next();
		assertNotNull(featureType);
		
		assertFalse(itr.hasNext());
		
		featureType = new FeatureType(
			featureType.getName(),
			featureType.getNativeName(),
			featureType.getTitle(),
			featureType.getAbstract(),
			featureType.getKeywords(),
			featureType.getAttributes().stream()
				.filter(attribute -> !attribute.getName().equals("name"))
				.collect(Collectors.toList()));
		
		rest.putFeatureType(workspace, dataStore, featureType).get();
		
		assertEquals(
			featureCount,
			getDocument(ns, "http://" + host + ":8080/geoserver/wfs?request=GetFeature&service=WFS&version=1.1.0&typeName=test")
				.nodes("wfs:FeatureCollection/gml:featureMembers/test:test").size());
	}

	private XPathHelper getDocument(Map<String, String> ns, String url) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Document document = db.parse(url);		
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();		
		t.transform(new DOMSource(document), new StreamResult(System.out));
		System.out.println();
		
		return XMLUtils.xpath(document, Optional.of(ns));
	}
	
	private boolean equals(Document a, Document b) {
		return false;
	}
	
	@Test
	public void testStyles() throws Exception {
		try(DirectoryStream<Path> stream = 
			Files.newDirectoryStream(Paths.get("C:\\Users\\copierrj.IDGIS-TEAM\\tmp\\gpo-styles"))) {
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			
			Set<QName> ignoreElements = new HashSet<>();
			//ignoreElements.add(new QName("http://www.opengis.net/sld", "StyledLayerDescriptor"));
			ignoreElements.add(new QName("http://www.opengis.net/sld", "Opacity"));
			ignoreElements.add(new QName("http://www.opengis.net/sld", "Name"));
			
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer t = tf.newTransformer();
			
			for(Path entry : stream) {
				String name = entry.getName(entry.getNameCount() - 1).toString().split("\\.")[0];
				
				System.out.println(name);
				
				try(InputStream is = Files.newInputStream(entry)) {
					Document testStyle = db.parse(is);
					rest.postStyle(new Style(name, testStyle)).get();
					
					Document storedStyle = rest.getStyle(name).get().get().getSld();
					t.transform(new DOMSource(storedStyle), new StreamResult(System.out));
					System.out.println();
					
					assertTrue(equals(testStyle, storedStyle));
				}
			}
		}
		
	}
}
