package nl.idgis.publisher.service.geoserver;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.h2.api.AggregateFunction;
import org.h2.server.pg.PgServer;

import com.ning.http.util.Base64;
import com.vividsolutions.jts.geom.Geometry;

import nl.idgis.publisher.utils.FileUtils;

public class TestServers {
	
	public static final int JETTY_PORT = 7000;
	public static final int PG_PORT = PgServer.DEFAULT_PORT;
	
	private Thread pgListenThread;
	private PgServer pgServer;
	private Server jettyServer;
	
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
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + TestServers.PG_PORT + "/test", "postgres", "postgres");		
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
}
