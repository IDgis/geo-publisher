package nl.idgis.publisher.service.geoserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.Logging;
import nl.idgis.publisher.utils.XMLUtils;
import nl.idgis.publisher.utils.XMLUtils.XPathHelper;

public class ExtGeoServerRestTest {
	
	private static final String SLD_NS = "http://www.opengis.net/sld";

	private final String host = "192.168.129.100";
	
	private final String username = "admin";
	
	private final String password= "geoserver";
	
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
		
		rest = new DefaultGeoServerRest(f, log, "http://" + host + ":8080/geoserver/", username, password);
		
		try(Connection connection = getConnection("postgres")) {
			try(Statement stmt = connection.createStatement()) {
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
		
		try(Connection connection = getConnection("postgres")) {
			try(Statement stmt = connection.createStatement()) {
				System.out.println("locks:");
				try(ResultSet rs = stmt.executeQuery(
						"select l.pid, sa.query, sa.application_name, l.mode, l.granted " +
						"from pg_catalog.pg_locks l " +
						"join pg_catalog.pg_stat_activity sa on sa.pid = l.pid " +
						"where l.pid <>  pg_backend_pid() "
					)) {
					
					ResultSetMetaData md = rs.getMetaData();
					while(rs.next()) {
						System.out.println(
							IntStream.rangeClosed(1, md.getColumnCount())
								.mapToObj(i -> rethrow( () -> rs.getString(i)))
								.map(str -> str == null || str.trim().length() == 0 ? "null" : str)
								.collect(Collectors.joining(" ")));
					}
				}
				
				try(ResultSet rs = stmt.executeQuery("select count(pg_terminate_backend(pid)) from pg_stat_activity where datname = 'test'")) {
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
		
		try(Connection connection = getConnection("test")) {
			try(Statement stmt = connection.createStatement()) {
				stmt.execute("create extension postgis");
				stmt.execute("create table test(id serial, name text, the_geom geometry(POINT, 28992))");
				
				try(PreparedStatement pstmt = connection.prepareStatement("insert into test(name, the_geom) select ?, st_setsrid(st_point(?, ?), 28992)")) {
					for(int i = 0; i < featureCount; i++) {
						pstmt.setString(1, "name" + i);
						pstmt.setInt(2, i);
						pstmt.setInt(3, i * i);
						pstmt.addBatch();
					}
					
					pstmt.executeBatch();
				}
				
				try(ResultSet rs = stmt.executeQuery("select count(*) from test")) {
					assertTrue(rs.next());
					assertEquals(featureCount, rs.getInt(1));
				}
				
				stmt.execute("analyze test");
				
				try(ResultSet rs = stmt.executeQuery("select ST_Estimated_Extent('test', 'the_geom')")) {
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
		ns.put("ows", "http://www.opengis.net/ows");
		ns.put("wfs", "http://www.opengis.net/wfs");
		ns.put("gml", "http://www.opengis.net/gml");
		ns.put("test", "http://test");
		
		assertEquals(
			Arrays.asList("test:test"),
			getDocument(ns, "http://" + host + ":8080/geoserver/wfs?request=GetCapabilities&service=WFS&version=1.1.0")		
				.strings("wfs:WFS_Capabilities/wfs:FeatureTypeList/wfs:FeatureType/wfs:Name"));
		
		String getFeatureRequest = "http://" + host + ":8080/geoserver/wfs?request=GetFeature&service=WFS&version=1.1.0&typeName=test";
		
		int noOfRequests = 20;
		for(int i = 0; i < noOfRequests; i++) {
			assertEquals(
				featureCount,
				getDocument(ns, getFeatureRequest)
					.nodes("wfs:FeatureCollection/gml:featureMembers/test:test").size());
		}
		
		try(Connection connection = getConnection("test"))  {
			try(Statement stmt = connection.createStatement()) {
				stmt.execute("alter table test drop column name");
			}
		}
		
		try(Connection connection = getConnection("test"))  {
			try(Statement stmt = connection.createStatement()) {
				try(ResultSet rs = stmt.executeQuery("select * from test")) {
					ResultSetMetaData md = rs.getMetaData();
					assertEquals(2, md.getColumnCount());
					assertEquals("id", md.getColumnName(1));
					assertEquals("the_geom", md.getColumnName(2));
					
					while(rs.next());
				}
			}
		}
		
		assertTrue(
			getDocument(ns, getFeatureRequest)
				.string("ows:ExceptionReport/ows:Exception/ows:ExceptionText")
				.isPresent());
		
		Iterator<FeatureType> itr = rest.getFeatureTypes(workspace, dataStore).get().iterator();
		assertTrue(itr.hasNext());
		
		featureType = itr.next();
		assertNotNull(featureType);
		
		assertEquals(
			Arrays.asList("id", "name", "the_geom"),
			featureType.getAttributes().stream()
				.map(Attribute::getName)
				.collect(Collectors.toList()));
		
		assertFalse(itr.hasNext());
		
		rest.reload().get();
		
		itr = rest.getFeatureTypes(workspace, dataStore).get().iterator();
		assertTrue(itr.hasNext());
		
		featureType = itr.next();
		assertNotNull(featureType);
		
		assertTrue(featureType.getAttributes().isEmpty());
		
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
	
	private InputStream getFile(String url) throws Exception {
		URLConnection connection = new URL(url).openConnection();
		connection.addRequestProperty(
			"Authorization",
			"Basic " + new String(Base64.encodeBase64((username + ":" + password).getBytes())));
		
		return connection.getInputStream();
	}

	private XPathHelper getDocument(Map<String, String> ns, String url) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		Document document = db.parse(getFile(url));
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();		
		t.transform(new DOMSource(document), new StreamResult(System.out));
		System.out.println();
		
		return XMLUtils.xpath(document, Optional.of(ns));		
	}
	
	private void normalize(Node n) {
		NodeList children = n.getChildNodes();
		List<Node> remove = new ArrayList<>();
		for(int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			switch(child.getNodeType()) {
				case Node.COMMENT_NODE:
					remove.add(child);
					break;
				case Node.TEXT_NODE:
					String content = child.getTextContent();
					String trimmedContent = content.trim();
					if(trimmedContent.isEmpty()) {
						remove.add(child);
					} else {
						if(!content.equals(trimmedContent)) {
							child.setTextContent(content);
						}
					}
			}
		}
		
		remove.forEach(n::removeChild);
		
		if(n.getNodeType() == Node.ELEMENT_NODE) {
			Element e = (Element)n;
			
			if(e.getNamespaceURI().equals(SLD_NS)) {
				String localName = e.getLocalName();
				if(localName.equals("Opacity")
					&& e.getTextContent().equals("1")) {
					e.getParentNode().removeChild(e);
					return;
				} else if(localName.equals("FeatureTypeStyle")) {
				
					Node firstChild = e.getFirstChild();
					if(firstChild.getNodeType() == Node.ELEMENT_NODE) {
						Element firstElement = (Element)firstChild;
						if(!firstElement.getLocalName().equals("Name") 
							|| !firstElement.getNamespaceURI().equals(SLD_NS)) {
							
							Document document = e.getOwnerDocument();
							Element nameElement = document.createElementNS(SLD_NS, "Name");
							nameElement.appendChild(document.createTextNode("name"));
							e.insertBefore(nameElement, firstChild);
						}
					}
				}
			}
		}
		
		for(int i = 0; i < children.getLength(); i++) {
			normalize(children.item(i));
		}
	}
	
	@Test
	public void testStyles() throws Exception {
		try(DirectoryStream<Path> stream = 
			Files.newDirectoryStream(Paths.get("C:\\Users\\copierrj.IDGIS-TEAM\\tmp\\gpo-styles"))) {
			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			
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
					
					normalize(testStyle);
					
					assertTrue(XMLUtils.equals(testStyle, storedStyle));
				}
			}
		}
		
	}
}
