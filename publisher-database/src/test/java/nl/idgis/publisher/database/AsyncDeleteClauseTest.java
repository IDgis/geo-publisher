package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AsyncDeleteClauseTest extends AbstractDatabaseHelperTest {
	
	@Test
	public void testExecute() throws Exception {
		insert(category)
			.set(category.identification, "testCategory")
			.set(category.name, "My Test Category!")
			.execute();
		
		assertTrue(query().from(category).exists());
		
		db.delete(category)
			.where(category.identification.eq("anotherCategory"))
			.execute().get();
		
		assertTrue(query().from(category).exists());
		
		db.delete(category)
			.where(category.identification.eq("testCategory"))
			.execute().get();
		
		assertFalse(query().from(category).exists());
		
		insert(category)
			.set(category.identification, "testCategory")
			.set(category.name, "My Test Category!")
			.execute();
		
		assertTrue(query().from(category).exists());
		
		db.delete(category)
			.execute().get();
		
		assertFalse(query().from(category).exists());
	}
}
