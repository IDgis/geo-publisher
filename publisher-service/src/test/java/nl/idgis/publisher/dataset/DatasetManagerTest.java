package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Iterator;

import org.junit.Test;

import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.Updated;

import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;

import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.AbstractServiceTest;

public class DatasetManagerTest extends AbstractServiceTest {

	@Test
	public void testRegisterNew() throws Exception {		 		
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		 
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", createTestDataset()), Registered.class);		
		
		assertTrue(
			query().from(category)
			.where(category.identification.eq("testCategory"))
			.exists());
		
		assertTrue(
			query().from(sourceDataset)
				.where(sourceDataset.identification.eq("testSourceDataset"))
				.exists());
		
		assertEquals(1,
			query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.where(sourceDataset.identification.eq("testSourceDataset"))
				.singleResult(sourceDatasetVersion.id.count()).intValue());
	}
	
	@Test
	public void testRegisterUpdate() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();	 
		
		// fill database with other source datasets
		for(int i = 0; i < 100; i++) {
			sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", createTestDataset("otherSourceDataset" + i)), Registered.class);
		}
		
		VectorDataset dataset = createTestDataset();		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Registered.class);		
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), AlreadyRegistered.class);
		
		Thread.sleep(1000); // createTestDataset() uses current time as revision date
		
		VectorDataset updatedDataset = createTestDataset();
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", updatedDataset), Updated.class);		
		
		assertEquals(2,
				query().from(sourceDatasetVersion)
					.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
					.where(sourceDataset.identification.eq("testSourceDataset"))
					.singleResult(sourceDatasetVersion.id.count()).intValue());
		
		Iterator<Timestamp> itr = query().from(sourceDatasetVersion)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
			.where(sourceDataset.identification.eq("testSourceDataset"))
			.orderBy(sourceDatasetVersion.id.asc())
			.list(sourceDatasetVersion.revision).iterator();
		
		assertTrue(itr.hasNext());
		
		Timestamp t = itr.next();
		assertEquals(dataset.getRevisionDate().getTime(), t.getTime());
		assertNotNull(t);
		
		assertTrue(itr.hasNext());
		t = itr.next();
		assertEquals(updatedDataset.getRevisionDate().getTime(), t.getTime());
		assertNotNull(t);
		
		assertFalse(itr.hasNext());
	}
}
