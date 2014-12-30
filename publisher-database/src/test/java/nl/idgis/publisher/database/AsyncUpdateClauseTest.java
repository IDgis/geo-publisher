package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import scala.concurrent.duration.Duration;

import akka.util.Timeout;

import nl.idgis.publisher.utils.SmartFuture;

public class AsyncUpdateClauseTest extends AbstractDatabaseTest {

	@Test
	public void testExecute() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();
		
		AsyncSQLUpdateClause update = new AsyncSQLUpdateClause(database, new Timeout(1, TimeUnit.SECONDS), dispatcher(), dataSource);
		
		SmartFuture<Long> future = update.set(dataSource.name, "newName")			
			.execute();
		
		Long affectedRows = future.get(Duration.create(2, TimeUnit.SECONDS));
		assertNotNull(affectedRows);
		assertEquals(new Long(1), affectedRows);
		
		assertEquals(
				"newName", 
				
				query().from(dataSource)
				.where(dataSource.identification.eq("id"))
				.singleResult(dataSource.name));
	}
	
	@Test
	public void testExecuteWhere() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();
	
		AsyncSQLUpdateClause update = new AsyncSQLUpdateClause(database, new Timeout(1, TimeUnit.SECONDS), dispatcher(), dataSource);
		
		SmartFuture<Long> future = update.set(dataSource.name, "newName")
			.where(dataSource.identification.eq("anotherId"))
			.execute();
		
		Long affectedRows = future.get(Duration.create(2, TimeUnit.SECONDS));
		assertNotNull(affectedRows);
		assertEquals(new Long(0), affectedRows);
		
		assertEquals(
				"name", 
				
				query().from(dataSource)
				.where(dataSource.identification.eq("id"))
				.singleResult(dataSource.name));
		
		update = new AsyncSQLUpdateClause(database, new Timeout(1, TimeUnit.SECONDS), dispatcher(), dataSource);		
		future = update.set(dataSource.name, "newName")
			.where(dataSource.identification.eq("id"))
			.execute();
			
		affectedRows = future.get(Duration.create(2, TimeUnit.SECONDS));
		assertNotNull(affectedRows);
		assertEquals(new Long(1), affectedRows);
		
		assertEquals(
				"newName", 
				
				query().from(dataSource)
				.where(dataSource.identification.eq("id"))
				.singleResult(dataSource.name));
	}
}
