package nl.idgis.publisher.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.ExtendedPostgresTemplates;
import nl.idgis.publisher.utils.FileUtils;
import nl.idgis.publisher.utils.JdbcUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;

import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public abstract class AbstractDatabaseTest {
	
	private static final File BASE_DIR = new File("target/test-database");
	
	private ActorSystem system;	
	private ExtendedPostgresTemplates templates;	
	private Connection connection;	
	private ActorRef database;
	
	@Before
	public void setUp() throws Exception {
		if(BASE_DIR.exists()) {			
			while(!FileUtils.delete(BASE_DIR)) {				
				Thread.sleep(10);
			}
		}
		
		String url = "jdbc:h2:" + BASE_DIR.getAbsolutePath() + "/publisher;DATABASE_TO_UPPER=false;MODE=PostgreSQL";		
		String user = "sa";
		String password = "";
		
		connection = DriverManager.getConnection(url, user, password);
		Statement stmt = connection.createStatement();
		JdbcUtils.runRev(stmt, "nl/idgis/publisher/database");
		stmt.close();
		
		templates = new ExtendedPostgresTemplates();
		
		Config databaseConfig = ConfigFactory.empty()
			.withValue("url", ConfigValueFactory.fromAnyRef(url))
			.withValue("templates", ConfigValueFactory.fromAnyRef("nl.idgis.publisher.database.ExtendedPostgresTemplates"))
			.withValue("user", ConfigValueFactory.fromAnyRef(user))
			.withValue("password", ConfigValueFactory.fromAnyRef(password));
		
		system = ActorSystem.create();
		database = system.actorOf(PublisherDatabase.props(databaseConfig));
	}
	
	protected SQLInsertClause insert(RelationalPath<?> entity) {
		return new SQLInsertClause(connection, templates, entity);
	}
	
	protected SQLUpdateClause update(RelationalPath<?> entity) {
		return new SQLUpdateClause(connection, templates, entity);
	}
	
	protected SQLDeleteClause delete(RelationalPath<?> entity) {
		return new SQLDeleteClause(connection, templates, entity);
	}
	
	protected SQLQuery query() {
		return new SQLQuery(connection, templates);
	}
	
	protected Object ask(Object msg) throws Exception {
		Future<Object> future = Patterns.ask(database, msg, 1000);
		return Await.result(future, Duration.create(1, TimeUnit.SECONDS));
	}
	
	@After
	public void shutdown() throws Exception {
		connection.close();		
		system.shutdown();
	}
}
