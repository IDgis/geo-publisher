package nl.idgis.publisher.monitor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A small Java application for testing the WMS services.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class WMSTest {
	
	public static void main(String[] args) throws Exception {
		int timeout = Integer.parseInt(getEnv("TIMEOUT"));
		
		if(args.length == 0) {
			throw new IllegalArgumentException("arguments missing");
		}
		
		switch(args[0]) {
			case "published":
				if(args.length == 1) {
					throw new IllegalArgumentException("argument for monitor command 'published' missing");
				}
				
				testPublishedServices(args[1]);
				break;
			case "staging":
				testStagingServices();
				break;
			default:
				throw new IllegalArgumentException("unknown monitor command: " + args[0]);
		}
		
		Thread.sleep(timeout);
		System.out.println("WMS CRITICAL: timeout after " + timeout + "ms");
		System.exit(2);
	}
	
	private static URL getCapabilitiesURL(String environmentUrl) throws Exception {
		return new URL(environmentUrl + "wms?service=WMS&request=GetCapabilities&version=1.3.0");
	}
	
	private static void testStagingServices() throws Exception {
		testServices(
			getCapabilitiesURL(getEnv("STAGING_ENVIRONMENT_URL")),
			getStagingServiceConfig());
	}

	private static void testPublishedServices(String environmentId) throws Exception {
		testServices(
			getCapabilitiesURL(getEnvironmentUrl(environmentId)), 
			getPublishedServiceConfig(environmentId));
	}
	
	private static void testServices(URL capabilitiesURL, Set<String> serviceConfig) {
		new Thread(() -> {
			doTestServices(capabilitiesURL, serviceConfig);
		}, "service-tester").start();
	}
	
	private static void doTestServices(URL capabilitiesURL, Set<String> serviceConfig) {
		StringBuilder error = new StringBuilder();
		
		try {
			Set<String> serviceContent = getServiceContent(capabilitiesURL);
			
			Set<String> missingLayers = serviceConfig.stream()
				.filter(layerName -> !serviceContent.contains(layerName))
				.collect(Collectors.toSet());
			
			Set<String> unexpectedLayers = serviceContent.stream()
				.filter(layerName -> !serviceConfig.contains(layerName))
				.collect(Collectors.toSet());
			
			if(!missingLayers.isEmpty()) {
				error.append(" missing layers: " 
					+ missingLayers.stream()
						.collect(Collectors.joining(", ")));
			}
			
			if(!unexpectedLayers.isEmpty()) {
				error.append(" unexpected layers: " 
					+ unexpectedLayers.stream()
						.collect(Collectors.joining(", ")));
			}
		} catch(RuntimeException e) {
			throw e;
		} catch(WMSException e) {
			error.append(" HTTP status: " + e.getResponseCode());
		} catch(Exception e) {
			error.append(" exception: " + e.getMessage());
		}
		
		if(error.length() == 0) {
			System.out.println("WMS OK");
			System.exit(0);
		} else {
			System.out.println("WMS CRITICAL:" + error);
			System.exit(2);
		}
	}

	private static String getEnvironmentUrl(String environmentId) throws Exception {
		try(Connection c = getConnection();
			PreparedStatement stmt = c.prepareStatement(getQuery("environment_url.sql"))) {
			stmt.setString(1, environmentId);
			try(ResultSet rs = stmt.executeQuery()) {
				while(rs.next()) {
					return rs.getString(1);
				}
			}
		}
		
		throw new IllegalStateException("environment not found: " + environmentId);
	}

	private static Set<String> getServiceContent(URL capabilitiesURL) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		HttpURLConnection urlConnection = (HttpURLConnection)capabilitiesURL.openConnection();
		
		DocumentBuilder db = dbf.newDocumentBuilder();
		
		// prevents output on stderr
		db.setErrorHandler(new ErrorHandler() {

			@Override
			public void warning(SAXParseException e) throws SAXException {
				
			}

			@Override
			public void error(SAXParseException e) throws SAXException {
				throw e;
			}

			@Override
			public void fatalError(SAXParseException e) throws SAXException {
				throw e;
			}
		});
		
		Document wmsCapabilities = db.parse(urlConnection.getInputStream());
		
		int responseCode = urlConnection.getResponseCode();
		if(responseCode != 200) {
			throw new WMSException(responseCode);
		}
		
		XPathHelper xpath = new XPathHelper()
			.bindNamespaceUri("wms", "http://www.opengis.net/wms")
			.bindNamespaceUri("xlink", "http://www.w3.org/1999/xlink");
		
		return
			xpath.getNodes(wmsCapabilities, "/wms:WMS_Capabilities/wms:Capability//wms:Layer/wms:Name")
				.map(Node::getTextContent)
				.collect(Collectors.toSet());
	}
	
	private static String getEnv(String name) {
		return Objects.requireNonNull(System.getenv(name), 
			name + " environment variable missing");
	}
	
	private static Set<String> getStagingServiceConfig() throws Exception {
		try(Connection c = getConnection();
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(getQuery("staging_service_config.sql"))) {
			
			return toLayerSet(rs);
		}
	}

	private static Set<String> getPublishedServiceConfig(String environmentId) throws Exception {
		try(Connection c = getConnection();
			PreparedStatement stmt = c.prepareStatement(getQuery("published_service_config.sql"))) {
			stmt.setString(1, environmentId);
			try(ResultSet rs = stmt.executeQuery()) {
				return toLayerSet(rs);
			}
		}
	}

	private static Set<String> toLayerSet(ResultSet rs) throws SQLException {
		Set<String> layers = new HashSet<>();
		
		while(rs.next()) {
			String service = rs.getString(1);
			String layerName = rs.getString(2);
			
			String scopedLayerName = service + ":" + layerName;
			if(!layers.add(scopedLayerName)) {
				int count = 2;
				while(!layers.add(scopedLayerName + "-" + count++));
			}
		}
		
		return layers;
	}

	private static Connection getConnection() throws SQLException {
		return DriverManager.getConnection(
			"jdbc:postgresql://" 
				+ getEnv("PG_HOST") 
				+ ":" 
				+ getEnv("PG_PORT") 
				+ "/" + getEnv("PG_DBNAME"), 
			getEnv("PG_USER"), 
			getEnv("PG_PASSWORD"));
	}

	private static String getQuery(String resource) throws IOException {
		String serviceConfigQuery = IOUtils.readLines(
			WMSTest.class.getResourceAsStream(resource),
			"utf-8").stream()
				.map(line -> line.replace("\n", ""))
				.collect(Collectors.joining(" "));
		return serviceConfigQuery;
	}
}
