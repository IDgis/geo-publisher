package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.mysema.query.Tuple;

import akka.util.Timeout;

import nl.idgis.publisher.utils.FutureUtils;

public class AsyncInsertClauseTest extends AbstractDatabaseTest {

	@Test
	public void testExecute() throws Exception {
		assertTrue(query().from(dataSource).notExists());
		
		CompletableFuture<Long> future = asyncInsert()
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

	private AsyncSQLInsertClause asyncInsert() {
		FutureUtils f = new FutureUtils(dispatcher(), new Timeout(1, TimeUnit.SECONDS));
		AsyncSQLInsertClause insert = new AsyncSQLInsertClause(database, f, dataSource);
		return insert;
	}
	
	@Test
	public void testExecuteWithKey() throws Exception {
		assertTrue(query().from(dataSource).notExists());
		
		CompletableFuture<Integer> future = asyncInsert()
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.executeWithKey(dataSource.id);
		
		Integer generatedKey = future.get(2, TimeUnit.SECONDS);
		assertNotNull(generatedKey);
		
		Integer queryResult = query()
			.from(dataSource)
			.singleResult(
				dataSource.id);
		
		assertNotNull(queryResult);
		assertEquals(queryResult, generatedKey);
	}
}
