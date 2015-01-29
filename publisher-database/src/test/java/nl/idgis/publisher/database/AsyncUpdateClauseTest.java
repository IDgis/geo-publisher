package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsyncUpdateClauseTest extends AbstractDatabaseHelperTest {

	@Test
	public void testExecute() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();
		
		CompletableFuture<Long> future = db.update(dataSource).set(dataSource.name, "newName")			
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
	
	@Test
	public void testExecuteWhere() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();	
		
		CompletableFuture<Long> future = db.update(dataSource).set(dataSource.name, "newName")
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
				
		future = db.update(dataSource).set(dataSource.name, "newName")
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
