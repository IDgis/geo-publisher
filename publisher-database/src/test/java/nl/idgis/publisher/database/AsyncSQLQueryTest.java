package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.utils.TypedList;

import org.junit.Test;

import com.mysema.query.Tuple;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.util.Timeout;

public class AsyncSQLQueryTest extends AbstractDatabaseTest {
	
	@Test
	public void testTuple() throws Exception {
		insertDataset();
		
		Future<TypedList<Tuple>> future = new AsyncSQLQuery(database, new Timeout(1, TimeUnit.SECONDS), dispatcher())
			.from(category)
			.list(category.id, category.name);
		
		Iterator<Tuple> itr = Await.result(future, Duration.create(2, TimeUnit.SECONDS)).iterator();
		
		assertTrue(itr.hasNext());
		assertEquals("testCategory", itr.next().get(category.name));
		assertFalse(itr.hasNext());
	}

	@Test
	public void testProjection() throws Exception {
		insertDataset();
		
		Future<TypedList<String>> future = new AsyncSQLQuery(database, new Timeout(1, TimeUnit.SECONDS), dispatcher())
			.from(category)
			.list(category.name);
		
		Iterator<String> itr = Await.result(future, Duration.create(2, TimeUnit.SECONDS)).iterator();
		
		assertTrue(itr.hasNext());
		assertEquals("testCategory", itr.next());
		assertFalse(itr.hasNext());
	}
}
