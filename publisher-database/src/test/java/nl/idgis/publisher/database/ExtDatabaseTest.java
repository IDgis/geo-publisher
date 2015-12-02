package nl.idgis.publisher.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.postgresql.util.PSQLException;

import org.apache.tomcat.jdbc.pool.DataSourceFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExtDatabaseTest {
	
	private final String host = "192.168.129.100";
	
	private String getUrl(String database) {
		return "jdbc:postgresql://" + host + ":5432/" + database;
	}
	
	private Connection getConnection(String database) throws Exception {
		Properties props = new Properties();
		props.setProperty("user", "postgres");
		props.setProperty("password", "postgres");
		// The tests require an exception when a query blocks,
		// setting a socket timeout results in such an exception.
		props.setProperty("socketTimeout", "2");
		props.setProperty("ApplicationName", "test"); 
		
		return DriverManager.getConnection(getUrl(database), props);
	}	

	@Before
	public void init() throws Exception {
		try(Connection connection = getConnection("postgres")) {
			try(Statement stmt = connection.createStatement()) {
				stmt.execute("create database test");
			}
		}
		
		try(Connection connection = getConnection("test")) {
			try(Statement stmt = connection.createStatement()) {
				stmt.execute("create table test(id serial, name text)");
			}
		}
	}
	
	@After
	public void clean() throws Exception {
		try(Connection connection = getConnection("postgres")) {
			try(Statement stmt = connection.createStatement()) {				
				// Terminate all connections.
				try(ResultSet rs = stmt.executeQuery("select count(pg_terminate_backend(pid)) from pg_stat_activity where datname = 'test'");) {
					assertTrue(rs.next());
					System.out.println("connections terminated: " + rs.getInt(1));
				}
				stmt.execute("drop database test");
			}
		}
	}
	
	@Test(expected=PSQLException.class)
	public void testRawJdbcLockFailure() throws Exception {
		try(Connection select = getConnection("test")) {
			select.setAutoCommit(false);
			try(Statement stmt = select.createStatement()) {
				try(ResultSet rs = stmt.executeQuery("select id, name from test")) {
					while(rs.next());
				}
			}
			
			// Because we queried table 'test' within the context of a transaction
			// the connection obtained (and still holds) a non exclusive 
			// lock on that table.
			
			try(Connection alterTable = getConnection("test")) {
				try(Statement stmt = alterTable.createStatement()) {
					// Results in an exception because we cannot obtain 
					// the required exclusive lock on the table.
					stmt.execute("alter table test drop column name");
				}
			}
		}
	}
	
	@Test(expected=PSQLException.class)
	public void testTomcatJdbcLockFailure() throws Exception {
		Properties props = new Properties();
		props.setProperty("driverClassName", "org.postgresql.Driver");
		props.setProperty("url", getUrl("test"));
		props.setProperty("username", "postgres");
		props.setProperty("password", "postgres");
		
		DataSourceFactory factory = new DataSourceFactory();
		DataSource dataSource = factory.createDataSource(props);
		
		try(Connection select = dataSource.getConnection()) {
			select.setAutoCommit(false); // Start transaction.
			try(Statement stmt = select.createStatement()) {
				try(ResultSet rs = stmt.executeQuery("select id, name from test");) {
					while(rs.next());
				}
			}
		}
		
		// Because we queried table test within the context of a transaction,
		// the connection 'select' obtained a non exclusive lock on that 
		// table and because the connection is returned to the pool
		// (= connection remains active) the transaction still.
		// holds the lock.
		
		try(Connection alterTable = getConnection("test")) {
			try(Statement stmt = alterTable.createStatement()) {
				// Results in an exception because we cannot obtain 
				// the required exclusive lock on table test.
				stmt.execute("alter table test drop column name");
			}
		}
	}
	
	@Test
	public void testTomcatJdbcLockSuccess() throws Exception {
		Properties props = new Properties();
		props.setProperty("driverClassName", "org.postgresql.Driver");
		props.setProperty("url", getUrl("test"));
		props.setProperty("username", "postgres");
		props.setProperty("password", "postgres");
		
		// Ensure that transactions are properly terminated.
		props.setProperty("defaultAutoCommit", "false");
		props.setProperty("rollbackOnReturn", "true");
		
		DataSourceFactory factory = new DataSourceFactory();
		DataSource dataSource = factory.createDataSource(props);
		
		try(Connection select = dataSource.getConnection()) {
			assertFalse(select.getAutoCommit()); // We should be in a transaction.
			try(Statement stmt = select.createStatement();) {
				try(ResultSet rs = stmt.executeQuery("select id, name from test")) {
					while(rs.next());
				}
			}
		}
		
		// Because we queried table test within the context of a transaction
		// the connection 'select' obtained a non exclusive lock until the
		// connection pool performs a rollback when the connection is 
		// returned to the pool.
		
		try(Connection alterTable = getConnection("test")) {
			try(Statement stmt = alterTable.createStatement()) {
				//We should be able to obtain the required exclusive lock.
				stmt.execute("alter table test drop column name");
			}
		}
	}	
	
	@Test
	public void testTomcatJdbcTerminatedConnections() throws Exception {
		Properties props = new Properties();
		props.setProperty("driverClassName", "org.postgresql.Driver");
		props.setProperty("url", getUrl("test"));
		props.setProperty("username", "postgres");
		props.setProperty("password", "postgres");
		props.setProperty("defaultAutoCommit", "false");
		props.setProperty("rollbackOnReturn", "true");
		
		// Ensure that the pool provides valid connections. 
		props.setProperty("testOnBorrow", "true");		
		props.setProperty("validationInterval", "0");
		
		DataSourceFactory factory = new DataSourceFactory();
		DataSource dataSource = factory.createDataSource(props);
		
		try(Connection test = getConnection("test")) {
			// Terminate all connections.
			try(Connection terminate = getConnection("postgres")) {
				assertTrue(terminate.isValid(1000));
				try(Statement stmt = terminate.createStatement()) {
					try(ResultSet rs = stmt.executeQuery(
						"select count(pg_terminate_backend(pid)) "
						+ "from pg_stat_activity where datname = 'test'")) {
						assertTrue(rs.next());
					}
				}
			}
			
			assertFalse(test.isValid(1000));
		}
		
		// The pool should return a valid (and working) connection. 
		try(Connection c = dataSource.getConnection()) {
			assertTrue(c.isValid(1000));
			try(Statement stmt = c.createStatement()) {
				try(ResultSet rs = stmt.executeQuery("select * from test")) {
					while(rs.next());
				}
			}
		}
	}
}
