package nl.idgis.publisher.dataset;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionLog.sourceDatasetVersionLog;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import nl.idgis.publisher.dataset.messages.AlreadyRegistered;
import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.service.DatabaseLog;
import nl.idgis.publisher.domain.service.DatasetLogType;
import nl.idgis.publisher.domain.service.UnavailableDataset;
import nl.idgis.publisher.domain.service.VectorDataset;

import nl.idgis.publisher.AbstractServiceTest;

public class DatasetManagerTest extends AbstractServiceTest {
	
	@Before
	public void dataSource() {
		insertDataSource();
	}
	
	@Test
	public void testRegisterNewUnavailableDataset() throws Exception {
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", createUnavailableDataset()), Registered.class);
		
		assertTrue(query().from(sourceDatasetVersion).exists());
		assertTrue(query().from(sourceDatasetVersionLog).exists());
	}
	
	@Test
	public void testRegisterUpdateUnavailableDataset() throws Exception {
		UnavailableDataset dataset = createUnavailableDataset();
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Registered.class);
		
		assertTrue(query().from(sourceDatasetVersion).exists());
		
		Set<Log> logs = new HashSet<>(dataset.getLogs());
		dataset = new UnavailableDataset(
			dataset.getId(), 
			dataset.getName(), 
			dataset.getCategoryId(), 
			dataset.getRevisionDate(), 
			logs);
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), AlreadyRegistered.class);
		
		logs = new HashSet<>();
		logs.add(Log.create(LogLevel.ERROR, DatasetLogType.TABLE_NOT_FOUND, new DatabaseLog("my_table")));
		dataset = new UnavailableDataset(
			dataset.getId(),
			dataset.getName(),
			dataset.getCategoryId(), 
			dataset.getRevisionDate(), 
			logs);
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Updated.class);
	}

	@Test
	public void testRegisterNewVectorDataset() throws Exception { 
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", createVectorDataset()), Registered.class);		
		
		assertTrue(
			query().from(category)
			.where(category.identification.eq("testCategory"))
			.exists());
		
		assertTrue(
			query().from(sourceDataset)
				.where(sourceDataset.identification.eq("testVectorDataset"))
				.exists());
		
		assertEquals(1,
			query().from(sourceDatasetVersion)
				.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
				.where(sourceDataset.identification.eq("testVectorDataset"))
				.singleResult(sourceDatasetVersion.id.count()).intValue());
	}
	
	@Test
	public void testRegisterUpdateVectorDataset() throws Exception {
		// fill database with other source datasets
		for(int i = 0; i < 100; i++) {
			sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", createVectorDataset("otherSourceDataset" + i)), Registered.class);
		}
		
		VectorDataset dataset = createVectorDataset();		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Registered.class);		
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), AlreadyRegistered.class);
		
		Thread.sleep(1000); // createTestDataset() uses current time as revision date
		
		VectorDataset updatedDataset = createVectorDataset();
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", updatedDataset), Updated.class);		
		
		assertEquals(2,
				query().from(sourceDatasetVersion)
					.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
					.where(sourceDataset.identification.eq("testVectorDataset"))
					.singleResult(sourceDatasetVersion.id.count()).intValue());
		
		Iterator<Timestamp> itr = query().from(sourceDatasetVersion)
			.join(sourceDataset).on(sourceDataset.id.eq(sourceDatasetVersion.sourceDatasetId))
			.where(sourceDataset.identification.eq("testVectorDataset"))
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
	
	@Test
	public void testRegisterUnavailableDatasetNoCategory() throws Exception {
		UnavailableDataset dataset = createUnavailableDataset();
		dataset = new UnavailableDataset(
				dataset.getId(), 
				dataset.getName(), 
				null, //categoryId removed 
				dataset.getRevisionDate(), 
				dataset.getLogs());
		
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), Registered.class);
		
		// verifies that the dataset manager is able to retrieve datasets without a categoryId 
		sync.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset), AlreadyRegistered.class);
	}
}
