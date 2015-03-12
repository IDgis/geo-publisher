package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

import org.junit.Before;
import org.junit.Test;

import com.mysema.query.Tuple;

import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

public class AsyncSQLQueryTest extends AbstractDatabaseTest {
	
	FutureUtils f;
	
	AsyncDatabaseHelper db;
	
	@Before
	public void setup() {
		LoggingAdapter log = Logging.getLogger(system, this);
		
		f = new FutureUtils(system, Timeout.apply(1, TimeUnit.SECONDS));
		db = new AsyncDatabaseHelper(database, f, log);
	}
	
	@Test
	public void testTuple() throws Exception {
		insertCategory();
		
		CompletableFuture<TypedList<Tuple>> future = db.query()
			.from(category)
			.list(category.id, category.name);
		
		Iterator<Tuple> itr = future.get().iterator();
		
		assertTrue(itr.hasNext());
		assertEquals("testCategory", itr.next().get(category.name));
		assertFalse(itr.hasNext());
	}

	@Test
	public void testProjection() throws Exception {
		insertCategory();
		
		CompletableFuture<TypedList<String>> future = db.query()
			.from(category)
			.list(category.name);
		
		Iterator<String> itr = future.get().iterator();
		
		assertTrue(itr.hasNext());
		assertEquals("testCategory", itr.next());
		assertFalse(itr.hasNext());
	}
	
	@Test
	public void testSingleResult() throws Exception {
		assertFalse(
				db.query()
						.from(category)
						.singleResult(category.id, category.name).get().isPresent());
		
		insertCategory();
		
		assertTrue(
				db.query()
						.from(category)
						.singleResult(category.id, category.name).get().isPresent());
	}

	private void insertCategory() {
		insert(category)
			.columns(category.identification, category.name)
			.values("categoryId", "testCategory")
			.execute();
	}
	
	@Test
	public void testExists() throws Exception {
		assertFalse(
				db.query()
						.from(category)
						.exists().get());
		
		insertCategory();
		
		assertTrue(
				db.query()
						.from(category)
						.exists().get());
	}
	
	@Test
	public void testNotExists() throws Exception {
		assertTrue(
				db.query()
						.from(category)
						.notExists().get());
		
		insertCategory();
		
		assertFalse(
				db.query()
						.from(category)
						.notExists().get());
	}
	
	@Test
	public void testCount() throws Exception {
		assertEquals(0, db.query().from(category).count().get().longValue());
		
		insertCategory();
		
		assertEquals(1, db.query().from(category).count().get().longValue());
	}
}
