package nl.idgis.publisher.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.ExtendedPostgresTemplates;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
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

import static org.junit.Assert.assertTrue;

public abstract class AbstractDatabaseTest {
	
	private static final File BASE_DIR = new File("target/test-database");
	
	private ActorSystem system;	
	private ExtendedPostgresTemplates templates;	
	private Connection connection;	
	private ActorRef database;
	
	@Before
	public void setUp() throws Exception {
		File dbDir;
		do {
			dbDir = new File(BASE_DIR, "" + Math.abs(new Random().nextInt()));
		} while(dbDir.exists());
		
		String url = "jdbc:h2:" + dbDir.getAbsolutePath() + "/publisher;DATABASE_TO_UPPER=false;MODE=PostgreSQL";		
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
	
	protected <T> T ask(Object msg, Class<T> resultType) throws Exception {
		Object result = ask(msg);		
		assertTrue("Unexpected result type: " + result.getClass(), resultType.isInstance(result));
		return resultType.cast(result);		
	}
	
	protected Object ask(Object msg) throws Exception {
		Future<Object> future = Patterns.ask(database, msg, 500000000);
		return Await.result(future, Duration.create(5, TimeUnit.HOURS));
	}
	
	@After
	public void shutdown() throws Exception {
		connection.close();		
		system.shutdown();
	}
	
	protected Dataset createTestDataset() {
		return createTestDataset("testSourceDataset");
	}

	protected Dataset createTestDataset(String id) {
		List<Column> columns = Arrays.asList(
				new Column("col0", Type.TEXT),
				new Column("col1", Type.NUMERIC));
		Table table = new Table("My Test Table", columns);
		
		Timestamp revision = new Timestamp(new Date().getTime());
		
		return new Dataset(id, "testCategory", table, revision);		
	}
	
	protected void executeJobs(Query query) throws Exception {
		for(Object msg : ask(query, List.class)) {
			assertTrue(msg instanceof JobInfo);			
			
			ask(new UpdateJobState((JobInfo)msg, JobState.SUCCEEDED));
		}
	}
}
