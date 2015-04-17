package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.mysema.query.Tuple;

public class AsyncInsertClauseTest extends AbstractDatabaseHelperTest {
	
	@Test
	public void testExecute() throws Exception {
		assertTrue(query().from(dataSource).notExists());
		
		CompletableFuture<Long> future = db.insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();
		
		Long affectedRows = future.get(2, TimeUnit.SECONDS);
		assertNotNull(affectedRows);
		assertEquals(new Long(1), affectedRows);
		
		Tuple queryResult = query()
			.from(dataSource)
			.singleResult(
					dataSource.identification,
					dataSource.name);
		
		assertNotNull(queryResult);
		assertEquals("id", queryResult.get(dataSource.identification));
		assertEquals("name", queryResult.get(dataSource.name));
	}	
	
	@Test
	public void testExecuteWithKey() throws Exception {
		assertTrue(query().from(dataSource).notExists());
		
		CompletableFuture<Optional<Integer>> future = db.insert(dataSource)
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.executeWithKey(dataSource.id);
		
		Integer generatedKey = future.get(2, TimeUnit.SECONDS).get();
		assertNotNull(generatedKey);
		
		Integer queryResult = query()
			.from(dataSource)
			.singleResult(
				dataSource.id);
		
		assertNotNull(queryResult);
		assertEquals(queryResult, generatedKey);
	}
	
	@Test
	public void testAddBatch() throws Exception {
		assertTrue(query().from(dataSource).notExists());
		
		List<Integer> keys =
			insert(dataSource)
				.set(dataSource.identification, "id-0")
				.set(dataSource.name, "name-0")	
				.addBatch()
				.set(dataSource.identification, "id-1")
				.set(dataSource.name, "name-1")
				.addBatch()
				.executeWithKeys(dataSource.id);
		
		assertEquals(2, query().from(dataSource).count());
		assertEquals(1, keys.size()); // first batch only (!)
		
		keys = 
			db.insert(dataSource)
				.set(dataSource.identification, "id-2")
				.set(dataSource.name, "name-2")	
				.addBatch()
				.set(dataSource.identification, "id-3")
				.set(dataSource.name, "name-3")
				.addBatch()
				.executeWithKeys(dataSource.id).get().list();
		
		assertEquals(4, query().from(dataSource).count());
		assertEquals(1, keys.size());
		
		assertEquals(
			Arrays.asList("id-0", "id-1", "id-2", "id-3"),
			query()
				.from(dataSource)
				.orderBy(dataSource.identification.asc())
				.list(dataSource.identification));
	}
}
