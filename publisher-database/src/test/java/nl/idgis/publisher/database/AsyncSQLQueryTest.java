package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.utils.SmartFuture;
import nl.idgis.publisher.utils.TypedList;

import org.junit.Test;

import com.mysema.query.Tuple;

import scala.concurrent.duration.Duration;

import akka.util.Timeout;

public class AsyncSQLQueryTest extends AbstractDatabaseTest {
	
	private AsyncSQLQuery asyncQuery() {
		return new AsyncSQLQuery(database, new Timeout(1, TimeUnit.SECONDS), dispatcher());
	}
	
	private <T> T result(SmartFuture<T> future) throws Exception {
		return future.get(Duration.create(2, TimeUnit.SECONDS));
	}
	
	@Test
	public void testTuple() throws Exception {
		insertDataset();
		
		SmartFuture<TypedList<Tuple>> future = asyncQuery()
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
		
		SmartFuture<TypedList<String>> future = asyncQuery()
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
