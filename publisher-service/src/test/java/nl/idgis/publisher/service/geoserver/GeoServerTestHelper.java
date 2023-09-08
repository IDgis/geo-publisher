package nl.idgis.publisher.service.geoserver;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import akka.event.LoggingAdapter;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.ServiceType;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.utils.FileUtils;
import nl.idgis.publisher.utils.FutureUtils;

public class GeoServerTestHelper {
	
	public static final int JETTY_PORT = 7000;

	private static final String DB_HOST = "localhost";

	private static final String DB_PORT = "49153";
			
	private Server jettyServer;
	
	private DocumentBuilder documentBuilder;
	
	private XPath xpath;
	
	public GeoServerTestHelper() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		documentBuilder = dbf.newDocumentBuilder();
		
		BiMap<String, String> namespaces = HashBiMap.create();
		namespaces.put("wms", "http://www.opengis.net/wms");
		namespaces.put("sld", "http://www.opengis.net/sld");
		
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
	
	public void start() throws Exception {
		for(int i = 60; i >= 0; i--) {
			Thread.sleep(1000);
			
			try(Connection c = DriverManager.getConnection("jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/postgres", "postgres", "postgres");
				Statement stmt = c.createStatement()) {
				stmt.execute("create database \"test\"");
				break;
			} catch(Exception e) { 
				if(i == 0) {
					throw new IllegalStateException("Failed to create test database", e);
				}
			}
		}
		
		try(Connection c = DriverManager.getConnection("jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/test", "postgres", "postgres");
			Statement stmt = c.createStatement()) {
			stmt.execute("create extension postgis");
		} catch(Exception e) { 
			throw new IllegalStateException("Failed to create postgis extention", e);
		}
		
		File dataDir = new File("build/geoserver-data");
		
		if(dataDir.exists()) {
			FileUtils.delete(dataDir);
		}
		
		dataDir.mkdir();
		
		String geoserverDataDir = dataDir.getAbsolutePath();
		System.setProperty("GEOSERVER_DATA_DIR", geoserverDataDir);
		
		jettyServer = new Server(JETTY_PORT);
		
		ClassList classlist = ClassList.setServerDefault(jettyServer);
		classlist.addAfter(
			"org.eclipse.jetty.webapp.FragmentConfiguration", 
			"org.eclipse.jetty.plus.webapp.EnvConfiguration", 
			"org.eclipse.jetty.plus.webapp.PlusConfiguration");
		
		WebAppContext context = new WebAppContext();
		File webXml = new File("build/geoserver/WEB-INF/web.xml");
		
		FileInputStream fis = new FileInputStream(webXml);
		DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();		
		Document d = db.parse(fis);
		fis.close();
		
		Element resourceRef = d.createElement("resource-ref");
		
		Element resourceRefName = d.createElement("res-ref-name");
		resourceRefName.appendChild(d.createTextNode("jdbc/db"));
		resourceRef.appendChild(resourceRefName);
		
		Element resourceRefType = d.createElement("res-type");
		resourceRefType.appendChild(d.createTextNode("javax.sql.DataSource"));
		resourceRef.appendChild(resourceRefType);
		
		Element resourceRefAuth = d.createElement("res-auth");
		resourceRefAuth.appendChild(d.createTextNode("Container"));
		resourceRef.appendChild(resourceRefAuth);
		
		d.getDocumentElement().appendChild(resourceRef);
		
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.transform(new DOMSource(d), new StreamResult(webXml));
		
		context.setDescriptor(webXml.getAbsolutePath());
		context.setResourceBase("build/geoserver");
		context.setContextPath("/");
		context.setParentLoaderPriority(false);
		
		BasicDataSource ds = new BasicDataSource();
		ds.setDriverClassName("org.postgresql.Driver");		
		ds.setUrl("jdbc:postgresql://" + DB_HOST + ":" + DB_PORT + "/test");
		ds.setUsername("postgres");
		ds.setPassword("postgres");
		
		new Resource(context, "jdbc/db", ds);
		
		jettyServer.setHandler(context);
		jettyServer.start();
		
		for(int i = 25; i >= 0; i--) {
			Thread.sleep(1000);
			
			try {
				HttpURLConnection connection = (HttpURLConnection)(new URL("http://localhost:" + JETTY_PORT + "/rest/workspaces.xml").openConnection());
				if(connection.getResponseCode() == 200) {
					break;
				}
			} catch(Exception e) { 
				if(i == 0) {
					throw new IllegalStateException("Failed to start GeoServer", e);
				}
			}
		}
	}
	
	public void stop() throws Exception {
		if (jettyServer != null) {
			jettyServer.stop();
		}
	}
	
	public void processNodeList(NodeList nodeList, Collection<String> retval) {
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
	
	public List<String> getText(Node node) {
		return getText(node.getChildNodes());
	}
	
	public List<String> getText(NodeList nodeList) {
		List<String> retval = new ArrayList<>();		
		processNodeList(nodeList, retval);		
		return retval;
	}
	
	public NodeList getNodeList(String expression, Node node) throws Exception {
		return (NodeList)xpath.evaluate(expression, node, XPathConstants.NODESET);
	}
	
	public void notExists(String expression, Node node) throws Exception {
		NodeList nodeList = getNodeList(expression, node);
		
		if(nodeList.getLength() != 0) {
			fail("result");
		}
	}
	
	public String getText(String expression, Node node) throws Exception {
		NodeList nodeList = getNodeList(expression, node);
		
		if(nodeList.getLength() == 0) {
			fail("no result");
		}
		
		if(nodeList.getLength() > 1) {
			fail("multiple results");
		}
		
		return nodeList.item(0).getTextContent();		
	}
	
	private String getServiceUrl(String serviceName, ServiceType serviceType) {
		return "http://localhost:" + JETTY_PORT + "/" + serviceName 
			+ "/" + serviceType.name().toLowerCase();
	}
	
	public Document getCapabilities(String serviceName, ServiceType serviceType, String version) throws SAXException, IOException {
		return documentBuilder.parse(getServiceUrl(serviceName, serviceType) + "?request=GetCapabilities&service=" 
			+ serviceType.name().toUpperCase() + "&version=" + version);
	}
	
	public Document getFeature(String serviceName, String typeName) throws SAXException, IOException {
		return documentBuilder.parse(getServiceUrl(serviceName, ServiceType.WFS) + "?request=GetFeature&service=WFS&version=1.1.0&typeName=" + typeName);
	}
	
	public GeoServerRest rest(FutureUtils f, LoggingAdapter log) throws Exception {
		return new DefaultGeoServerRest(f, log, "http://localhost:" + GeoServerTestHelper.JETTY_PORT + "/", "admin", "geoserver");
	}
	
	public void clean(FutureUtils f, LoggingAdapter log) throws Exception {
		GeoServerRest service = rest(f, log); 
		for(Workspace workspace : service.getWorkspaces().get()) {
			service.deleteWorkspace(workspace).get();
		}
		
		service.getStyleNames().get()
			.forEach(service::deleteStyle);
		
		service.close();
	}

	public String getDbPort() {
		return DB_PORT;
	}

	public String getDbHost() {
		return DB_HOST;
	}
}
