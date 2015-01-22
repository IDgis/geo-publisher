package nl.idgis.publisher.database;

import org.junit.Test;

import nl.idgis.publisher.utils.FutureUtils;

import static nl.idgis.publisher.database.QCategory.category;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AsyncDeleteClauseTest extends AbstractDatabaseTest {

	@Test
	public void testExecute() throws Exception {
		insert(category)
			.set(category.identification, "testCategory")
			.set(category.name, "My Test Category!")
			.execute();
		
		assertTrue(query().from(category).exists());
		
		FutureUtils f = new FutureUtils(system.dispatcher());
		
		new AsyncSQLDeleteClause(database, f, category)
			.where(category.identification.eq("anotherCategory"))
			.execute().get();
		
		assertTrue(query().from(category).exists());
		
		new AsyncSQLDeleteClause(database, f, category)
			.where(category.identification.eq("testCategory"))
			.execute().get();
		
		assertFalse(query().from(category).exists());
		
		insert(category)
			.set(category.identification, "testCategory")
			.set(category.name, "My Test Category!")
			.execute();
		
		assertTrue(query().from(category).exists());
		
		new AsyncSQLDeleteClause(database, f, category)			
			.execute().get();
		
		assertFalse(query().from(category).exists());
	}
}
