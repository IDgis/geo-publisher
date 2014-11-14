package nl.idgis.publisher.service;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.h2.server.pg.PgServer;

import nl.idgis.publisher.utils.FileUtils;

public class TestServers {
	
	public static final int JETTY_PORT = 7000;
	public static final int PG_PORT = PgServer.DEFAULT_PORT;
	
	private Thread pgListenThread;
	private PgServer pgServer;
	private Server jettyServer;

	public void start() throws Exception {
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
		
		Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + PG_PORT + "/test", "postgres", "postgres");
		
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
	
	public void stop() throws Exception {
		jettyServer.stop();
		pgServer.stop();
		
		pgListenThread.interrupt();
		pgListenThread.join();
	}
}
