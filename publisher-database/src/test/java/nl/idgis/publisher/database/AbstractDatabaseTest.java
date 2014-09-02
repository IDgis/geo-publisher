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
import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.utils.JdbcUtils;
import nl.idgis.publisher.utils.TypedIterable;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;

import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.dml.SQLDeleteClause;
import com.mysema.query.sql.dml.SQLInsertClause;
import com.mysema.query.sql.dml.SQLUpdateClause;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertTrue;

public abstract class AbstractDatabaseTest {
	
	private static final File BASE_DIR = new File("target/test-database");
	
	private ActorSystem system;	
	private ExtendedPostgresTemplates templates;	
	private Connection connection;	
	
	protected ActorRef database;
	
	@Before
	public void database() throws Exception {
		File dbDir;
		do {
			dbDir = new File(BASE_DIR, "" + Math.abs(new Random().nextInt()));
		} while(dbDir.exists());
		
		String url = "jdbc:h2:" + dbDir.getAbsolutePath() + "/publisher;DATABASE_TO_UPPER=false;MODE=PostgreSQL;DB_CLOSE_ON_EXIT=false";		
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
		
		Config akkaConfig = ConfigFactory.empty()
			.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
			.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
		
		system = ActorSystem.create("test", akkaConfig);
		database = actorOf(PublisherDatabase.props(databaseConfig), "database");
	}
	
	protected ActorRef actorOf(Props props, String name) {
		return system.actorOf(props, name);
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
	
	protected <T> T askAssert(ActorRef actorRef, Object msg, Class<T> resultType) throws Exception {
		Object result = ask(actorRef, msg);		
		assertTrue("Unexpected result type: " + result.getClass(), resultType.isInstance(result));
		return resultType.cast(result);
	}
	
	protected Object ask(ActorRef actorRef, Object msg) throws Exception {
		Future<Object> future = Patterns.ask(actorRef, msg, 5000);
		return Await.result(future, Duration.create(5, TimeUnit.MINUTES));
	}
	
	@After
	public void shutdown() throws Exception {
		connection.close();		
		system.shutdown();
	}
	
	protected int insertDataSource(String dataSourceId) {
		Integer retval = query().from(dataSource)
			.where(dataSource.identification.eq(dataSourceId))
			.singleResult(dataSource.id);
		
		if(retval != null) {
			return retval;
		}
		
		return insert(dataSource)
			.set(dataSource.identification, dataSourceId)
			.set(dataSource.name, "My Test DataSource")
			.executeWithKey(dataSource.id);
	}
	
	protected void insertDataset() throws Exception {
		insertDataset("testDataset");
	}
		
	protected void insertDataset(String datasetId) throws Exception {
		insertDataSource();
		
		Dataset testDataset = createTestDataset();
		ask(database, new RegisterSourceDataset("testDataSource", testDataset));
		
		Table testTable = testDataset.getTable();
		ask(database, new CreateDataset(
				datasetId, 
				"My Test Dataset", 
				testDataset.getId(), 
				testTable.getColumns(), 
				"{ \"expression\": null }"));
	}
	
	protected int insertDataSource() {
		return insertDataSource("testDataSource");
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
		TypedIterable<?> iterable = askAssert(database, query, TypedIterable.class);
		assertTrue(iterable.contains(JobInfo.class));
		for(JobInfo job : iterable.cast(JobInfo.class)) {
			ask(database, new UpdateJobState(job, JobState.SUCCEEDED));
		}
	}
}
