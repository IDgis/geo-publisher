package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

import org.junit.Test;

import com.mysema.query.Tuple;

import akka.util.Timeout;

public class AsyncSQLQueryTest extends AbstractDatabaseTest {
	
	private AsyncSQLQuery asyncQuery() {
		FutureUtils f = new FutureUtils(dispatcher(), new Timeout(1, TimeUnit.SECONDS));
		return new AsyncSQLQuery(database, f);
	}
	
	private <T> T result(CompletableFuture<T> future) throws Exception {
		return future.get(2, TimeUnit.SECONDS);
	}
	
	@Test
	public void testTuple() throws Exception {
		insertDataset();
		
		CompletableFuture<TypedList<Tuple>> future = asyncQuery()
			.from(category)
			.list(category.id, category.name);
		
		Iterator<Tuple> itr = result(future).iterator();
		
		assertTrue(itr.hasNext());
		assertEquals("testCategory", itr.next().get(category.name));
		assertFalse(itr.hasNext());
	}

	@Test
	public void testProjection() throws Exception {
		insertDataset();
		
		CompletableFuture<TypedList<String>> future = asyncQuery()
			.from(category)
			.list(category.name);
		
		Iterator<String> itr = result(future).iterator();
		
		assertTrue(itr.hasNext());
		assertEquals("testCategory", itr.next());
		assertFalse(itr.hasNext());
	}
	
	@Test
	public void testSingleResult() throws Exception {
		assertNull(
			result(
					asyncQuery()
						.from(category)
						.singleResult(category.id, category.name)));
		
		insertDataset();
		
		assertNotNull(
				result(
						asyncQuery()
							.from(category)
							.singleResult(category.id, category.name)));
	}
	
	@Test
	public void testExists() throws Exception {
		assertFalse(
			result(
					asyncQuery()
						.from(category)
						.exists()));
		
		insertDataset();
		
		assertTrue(
				result(
					asyncQuery()
						.from(category)
						.exists()));
	}
	
	@Test
	public void testNotExists() throws Exception {
		assertTrue(
			result(
					asyncQuery()
						.from(category)
						.notExists()));
		
		insertDataset();
		
		assertFalse(
				result(
					asyncQuery()
						.from(category)
						.notExists()));
	}
}
