package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import akka.util.Timeout;

import nl.idgis.publisher.utils.FutureUtils;

public class AsyncUpdateClauseTest extends AbstractDatabaseTest {

	@Test
	public void testExecute() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();
		
		CompletableFuture<Long> future = asyncUpdate().set(dataSource.name, "newName")			
			.execute();
		
		Long affectedRows = future.get(2, TimeUnit.SECONDS);
		assertNotNull(affectedRows);
		assertEquals(new Long(1), affectedRows);
		
		assertEquals(
				"newName", 
				
				query().from(dataSource)
				.where(dataSource.identification.eq("id"))
				.singleResult(dataSource.name));
	}

	private AsyncSQLUpdateClause asyncUpdate() {
		FutureUtils f = new FutureUtils(dispatcher(), new Timeout(1, TimeUnit.SECONDS));
		AsyncSQLUpdateClause update = new AsyncSQLUpdateClause(database, f, dataSource);
		return update;
	}
	
	@Test
	public void testExecuteWhere() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();	
		
		CompletableFuture<Long> future = asyncUpdate().set(dataSource.name, "newName")
			.where(dataSource.identification.eq("anotherId"))
			.execute();
		
		Long affectedRows = future.get(2, TimeUnit.SECONDS);
		assertNotNull(affectedRows);
		assertEquals(new Long(0), affectedRows);
		
		assertEquals(
				"name", 
				
				query().from(dataSource)
				.where(dataSource.identification.eq("id"))
				.singleResult(dataSource.name));
				
		future = asyncUpdate().set(dataSource.name, "newName")
			.where(dataSource.identification.eq("id"))
			.execute();
			
		affectedRows = future.get(2, TimeUnit.SECONDS);
		assertNotNull(affectedRows);
		assertEquals(new Long(1), affectedRows);
		
		assertEquals(
				"newName", 
				
				query().from(dataSource)
				.where(dataSource.identification.eq("id"))
				.singleResult(dataSource.name));
	}
}
