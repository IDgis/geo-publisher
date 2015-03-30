package nl.idgis.publisher.database;

import java.util.Optional;

import org.junit.Test;

import static nl.idgis.publisher.database.QCategory.category;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AsyncTransactionRefTest extends AbstractDatabaseHelperTest {

	@Test
	public void testTransactionRef() throws Exception {
		db.transactional(tx -> {
			return tx.insert(category)
				.set(category.identification, "id")
				.set(category.name, "name")
				.executeWithKey(category.id).thenCompose(categoryId -> {
					return db.query().from(category)
						.where(category.id.eq(categoryId.get()))
						.exists().thenCompose(otherTransactionExists -> {
							assertFalse(otherTransactionExists);							
							
							return tx.query().from(category)
								.where(category.id.eq(categoryId.get()))
								.exists().thenCompose(sameTransactionExists -> {
									assertTrue(sameTransactionExists);
									
									return db.transactional(Optional.of(tx.getTransactionRef()), tx2 -> 
										tx2.query().from(category)
											.where(category.id.eq(categoryId.get()))
											.exists().thenApply(boundTransactionExists -> {
										assertTrue(boundTransactionExists);
										
										return null;
									}));
									
								});
						});
				});
		}).get();
	}
}
