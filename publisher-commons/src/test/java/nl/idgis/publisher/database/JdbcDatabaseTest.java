package nl.idgis.publisher.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorIdentity;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Identify;
import akka.actor.Props;
import akka.actor.Terminated;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.recorder.messages.Watch;
import nl.idgis.publisher.recorder.messages.Watching;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class JdbcDatabaseTest {
	
	private static final int POOL_SIZE = 10;
	
	private static enum SqlQueryType {
		QUERY,
		UPDATE
	}
	
	private static class SqlQuery extends Query {		
			
		private static final long serialVersionUID = 3181270806726455483L;

		private final String sql;
		
		private final SqlQueryType type; 
		
		public SqlQuery(String sql, SqlQueryType type) {
			this.sql = sql;
			this.type = type;
		}
		
		public String getSql() {
			return sql;
		}

		public SqlQueryType getType() {
			return type;
		}

		@Override
		public String toString() {
			return "SqlQuery [sql=" + sql + ", type=" + type + "]";
		}
		
	}
	
	public static class TestTransaction extends JdbcTransaction {

		public TestTransaction(Connection connection) {
			super(connection);
		}
		
		public static Props props(Connection connection) {
			return Props.create(TestTransaction.class, connection);
		}
		
		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		protected Object executeQuery(Query query) throws Exception {
			if(query instanceof SqlQuery) {
				SqlQuery sqlQuery = (SqlQuery)query;
				
				Statement stmt = connection.createStatement();
				
				Object retval = null;
				switch(sqlQuery.getType()) {
					case QUERY:
						ResultSet rs = stmt.executeQuery(sqlQuery.getSql());
						
						ResultSetMetaData md = rs.getMetaData();
						int columnCount = md.getColumnCount();
						
						List<TypedList<Object>> records = new ArrayList<>();
						while(rs.next()) {
							List<Object> columns = new ArrayList<>();
							for(int i = 1; i <= columnCount; i++) {
								columns.add(rs.getObject(i));
							}
							
							records.add(new TypedList<Object>(Object.class, columns));
						}
						
						rs.close();
						
						retval = new TypedList(TypedList.class, records);
						
						break;
					case UPDATE:
						retval = stmt.executeUpdate(sqlQuery.getSql());
						
						break;
				}
				
				return retval;
			} else {
				return null;
			}
		}
		
	}
	
	public static class TestDatabase extends JdbcDatabase {

		public TestDatabase(Config config, String poolName, int poolSize) {
			super(config, poolName, poolSize);
		}
		
		public static Props props(Config config, String poolName, int poolSize) {
			return Props.create(TestDatabase.class, config, poolName, poolSize);
		}

		@Override
		protected Props createTransaction(Connection connection) { 
			return TestTransaction.props(connection);
		}		
	}
	
	ActorSystem actorSystem;
	
	ActorRef database;
	
	FutureUtils f;

	@Before
	public void startup() {
		actorSystem = ActorSystem.create();
		
		Config config = ConfigFactory.empty()
			.withValue("driver", ConfigValueFactory.fromAnyRef("org.h2.Driver"))
			.withValue("url", ConfigValueFactory.fromAnyRef("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"))
			.withValue("user", ConfigValueFactory.fromAnyRef(""))
			.withValue("password", ConfigValueFactory.fromAnyRef(""));
		
		database = actorSystem.actorOf(TestDatabase.props(config, "test", POOL_SIZE));
		
		f = new FutureUtils(actorSystem.dispatcher());
	}
	
	@After
	public void shutdown() throws Exception {
		ActorRef transaction = f.ask(database, new StartTransaction(), TransactionCreated.class).get().getActor();
		f.ask(transaction, new SqlQuery("shutdown", SqlQueryType.UPDATE)).get();
		
		actorSystem.shutdown();
	}
	
	@Test
	public void testQuery() throws Exception {
		TypedList<?> result = f.ask(database, new SqlQuery("select 1", SqlQueryType.QUERY), TypedList.class).get();
		assertTrue(result.contains(TypedList.class));
		
		@SuppressWarnings("rawtypes")
		Iterator<TypedList> records = result.cast(TypedList.class).iterator();
		assertTrue(records.hasNext());
		
		TypedList<?> record = records.next();
		Iterator<Object> columns = record.cast(Object.class).iterator();		
		assertTrue(columns.hasNext());
		assertEquals(1, columns.next());
		assertFalse(columns.hasNext());
		
		assertFalse(records.hasNext());
	}
	
	@Test
	public void testTransactionChildActors() throws Exception {
		final int numberOfTransactions = 2;
		
		Set<ActorRef> transactions = new HashSet<>();
		for(int i = 0; i < numberOfTransactions; i++) {
			transactions.add(f.ask(database, new StartTransaction(), TransactionCreated.class).get().getActor());
		}
		
		ActorRef identityRecorder = actorSystem.actorOf(AnyRecorder.props());
		ActorSelection.apply(database, "*").tell(new Identify("Hello!"), identityRecorder);

		f.ask(identityRecorder, new Wait(numberOfTransactions), Waited.class).get();
		
		Recording identityRecording = f.ask(identityRecorder, new GetRecording(), Recording.class).get();		
		
		ActorRef deadWatch = actorSystem.actorOf(AnyRecorder.props());
		for(int i = 0; i < numberOfTransactions; i++) {
			identityRecording
				.assertHasNext()
				.assertNext(ActorIdentity.class, actorIdentify -> {
					ActorRef transaction = actorIdentify.getRef();
					assertNotNull(transaction);
					assertTrue(transactions.contains(transaction));
					
					f.ask(deadWatch, new Watch(transaction), Watching.class);
				});
		}
		
		identityRecording.assertNotHasNext();
		
		for(ActorRef transaction : transactions) {
			f.ask(transaction, new Commit(), Ack.class).get();
		}
		
		f.ask(deadWatch, new Wait(2), Waited.class).get();
		Recording deadWatchRecording = f.ask(deadWatch, new GetRecording(), Recording.class).get();
		
		for(int i = 0; i < numberOfTransactions; i++) {
			deadWatchRecording
				.assertHasNext()
				.assertNext(Terminated.class, terminated -> {
					assertTrue(transactions.contains(terminated.getActor()));
				});
		}
		
		deadWatchRecording.assertNotHasNext();
	}
	
	@Test
	public void testTransactionRollback() throws Exception {
		f.ask(database, new SqlQuery("create table test(id integer)", SqlQueryType.UPDATE), Integer.class).get();
		
		ActorRef transaction = f.ask(database, new StartTransaction(), TransactionCreated.class).get().getActor();
		
		f.ask(transaction, new SqlQuery("insert into test(id) values(42)", SqlQueryType.UPDATE), Integer.class).get();		
		f.ask(transaction, new Rollback(), Ack.class).get();
		
		assertFalse(f.ask(database, new SqlQuery("select id from test", SqlQueryType.QUERY), TypedList.class).get()
			.iterator().hasNext());
	}
	
	@Test
	public void testTransactionCommit() throws Exception {
		f.ask(database, new SqlQuery("create table test(id integer)", SqlQueryType.UPDATE), Integer.class).get();
		
		ActorRef transaction = f.ask(database, new StartTransaction(), TransactionCreated.class).get().getActor();
		
		f.ask(transaction, new SqlQuery("insert into test(id) values(42)", SqlQueryType.UPDATE), Integer.class).get();
		
		assertFalse(f.ask(database, new SqlQuery("select id from test", SqlQueryType.QUERY), TypedList.class).get()
			.iterator().hasNext());
		
		f.ask(transaction, new Commit(), Ack.class).get();
		
		assertTrue(f.ask(database, new SqlQuery("select id from test", SqlQueryType.QUERY), TypedList.class).get()
			.iterator().hasNext());
	}
	
	@Test
	public void testTransactionCrash() throws Exception {
		ActorRef transaction = f.ask(database, new StartTransaction(), TransactionCreated.class).get().getActor();
		
		Throwable t = f.ask(transaction, new SqlQuery("invalid sql", SqlQueryType.QUERY), Failure.class).get().getCause();
		assertNotNull(t);
		assertTrue(t instanceof SQLException);
		
		f.ask(transaction, new SqlQuery("select 42", SqlQueryType.QUERY)).get(); // unclear if this should also result in Failure (it doesn't in h2)
		
		ActorRef deadWatch = actorSystem.actorOf(AnyRecorder.props());
		f.ask(deadWatch, new Watch(transaction), Watching.class).get();
		
		f.ask(transaction, new Rollback(), Ack.class).get();
		
		f.ask(deadWatch, new Wait(1), Waited.class).get();
		f.ask(deadWatch, new GetRecording(), Recording.class).get()
			.assertHasNext()
			.assertNext(Terminated.class, terminated -> {
				assertEquals(transaction, terminated.getActor());
			})
			.assertNotHasNext();
	}
	
	@Test
	public void testTooManyTransactions() throws Exception {
		final int numberOfTransactions = POOL_SIZE + 1;
		
		Set<ActorRef> transactions = new HashSet<>();
		for(int i = 0; i < numberOfTransactions; i++) {
			if(i < POOL_SIZE) {
				transactions.add(f.ask(database, new StartTransaction(), TransactionCreated.class).get().getActor());
			} else {
				f.ask(database, new StartTransaction(), Failure.class).get();
			}
		}
		
		for(ActorRef transaction : transactions) {
			f.ask(transaction, new Commit(), Ack.class);
		}
	}
}
