package nl.idgis.publisher.service.geoserver;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
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
import nl.idgis.publisher.utils.FutureUtils;

public class GeoServerTestHelper {
	
	private static final String DB_HOST = "localhost";
	
	private static final String DATASTORE_DB_HOST = "postgis-test";

	private static final int GEOSERVER_PORT = 8080;

	private static final String DB_PORT = "49153";
	
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
		for(int i = 25; i >= 0; i--) {
			Thread.sleep(1000);
			
			try {
				HttpURLConnection connection = (HttpURLConnection)(new URL("http://localhost:" + getGeoserverPort() + "/geoserver/rest/workspaces.xml").openConnection());
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
		return "http://localhost:" + getGeoserverPort() + "/geoserver/" + serviceName 
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
		return new DefaultGeoServerRest(f, log, "http://localhost:" + getGeoserverPort() + "/geoserver/", "admin", "geoserver");
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
	
	public int getGeoserverPort() {
		return GEOSERVER_PORT;
	}

	public String getDatastoreDbHost() {
		return DATASTORE_DB_HOST;
	}

	public String getDbHost() {
		return DB_HOST;
	}
}
