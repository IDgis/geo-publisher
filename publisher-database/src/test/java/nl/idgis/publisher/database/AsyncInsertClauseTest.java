package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.mysema.query.Tuple;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.util.Timeout;

public class AsyncInsertClauseTest extends AbstractDatabaseTest {

	@Test
	public void testExecute() throws Exception {
		assertTrue(query().from(dataSource).notExists());
		
		AsyncSQLInsertClause insert = new AsyncSQLInsertClause(database, new Timeout(1, TimeUnit.SECONDS), dispatcher(), dataSource);
		
		Future<Long> future = insert
			.set(dataSource.identification, "id")
			.set(dataSource.name, "name")		
			.execute();
		
		Long affectedRows = Await.result(future, Duration.create(2, TimeUnit.SECONDS));
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
}
