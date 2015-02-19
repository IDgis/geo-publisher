package nl.idgis.publisher.service.geoserver;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.h2.api.AggregateFunction;
import org.h2.server.pg.PgServer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ning.http.util.Base64;
import com.vividsolutions.jts.geom.Geometry;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.ServiceType;
import nl.idgis.publisher.service.geoserver.rest.Workspace;
import nl.idgis.publisher.utils.FileUtils;
import nl.idgis.publisher.utils.FutureUtils;

public class GeoServerTestHelper {
	
	public static final int JETTY_PORT = 7000;
	public static final int PG_PORT = PgServer.DEFAULT_PORT;
	
	private Thread pgListenThread;
	
	private PgServer pgServer;
	
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
	
	public static String PostGIS_Lib_Version() {
		return "2.1.5";
	}
	
	public static Geometry ST_Force_2D(Geometry geometry) {
		return geometry;
	}
	
	public static String encode(byte[] b, String method) {
		return Base64.encode(b);
	}
	
	public static byte[] ST_Estimated_Extent(String schemaName, String tableName, String geocolumnName) {
		return null;
	}
	
	public static class ST_Extent implements AggregateFunction {

		@Override
		public void init(Connection conn) throws SQLException {
			
		}

		@Override
		public int getType(int[] inputTypes) throws SQLException {
			return Types.BLOB;
		}

		@Override
		public void add(Object value) throws SQLException {
			
		}

		@Override
		public Object getResult() throws SQLException {			
			return null;
		}
		
	}

	public void start() throws Exception {
		pgServer = new PgServer();
		
		File baseDir = new File("target/geoserver-database");
		
		if(baseDir.exists()) {
			FileUtils.delete(baseDir);
		}
		
		pgServer.init(/*"-trace", */"-pgPort", "" + PG_PORT, "-baseDir", baseDir.getAbsolutePath());
		
		pgServer.start();
		
		pgListenThread = new Thread() {
			
			@Override
			public void run() {
				pgServer.listen();
			}
		};
		
		pgListenThread.start();
		
		// enable GeoDB
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + GeoServerTestHelper.PG_PORT + "/test", "postgres", "postgres");		
		Statement stmt = connection.createStatement();		
		stmt.execute("create alias if not exists init_geo_db for \"geodb.GeoDB.InitGeoDB\"");
		stmt.execute("call init_geo_db()");
		
		// add missing PostGIS functions
		for(String function : new String[]{"PostGIS_Lib_Version", "ST_Force_2D", "ST_Estimated_Extent", "encode"}) {
			stmt.execute("create alias " + function + " for \"" + getClass().getCanonicalName() + "." + function + "\"");
		}

		// disable ST_Extent
		stmt.execute("drop aggregate ST_Extent");		
		stmt.execute("create aggregate ST_Extent for \"" + getClass().getCanonicalName() + "$ST_Extent\"");
		
		// add 'geometry' type to pg_type 
		stmt.execute("merge into pg_catalog.pg_type select 705 oid, 'geometry' typname, "
				+ "(select oid from pg_catalog.pg_namespace where nspname = 'pg_catalog') typnamespace, "
				+ "-1 typlen, 'c' typtype, 0 typbasetype, -1 typtypmod, false typnotnull, null typinput "
				+ "from INFORMATION_SCHEMA.type_info where pos = 0");
		
		// create missing geography_columns table
		stmt.execute("create table geography_columns ("
				+ "f_table_catalog text, "
				+ "f_table_schema text, "
				+ "f_table_name text, "
				+ "f_geography_column text, "
				+ "coord_dimension integer, "
				+ "srid integer, "
				+ "type text "
				+ ")");

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
	
	public void stop() throws Exception {
		jettyServer.stop();
		pgServer.stop();
		
		pgListenThread.interrupt();
		pgListenThread.join();
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
	
	public void clean(FutureUtils f, LoggingAdapter log) throws Exception {
		GeoServerRest service = new DefaultGeoServerRest(f, log, "http://localhost:" + GeoServerTestHelper.JETTY_PORT + "/", "admin", "geoserver");
		for(Workspace workspace : service.getWorkspaces().get()) {
			service.deleteWorkspace(workspace).get();
		}
		service.close();
	}
}
