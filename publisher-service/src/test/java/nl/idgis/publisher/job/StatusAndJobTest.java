package nl.idgis.publisher.job;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedIterable;

import org.junit.Test;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static nl.idgis.publisher.utils.TestPatterns.ask;

public class StatusAndJobTest extends AbstractJobManagerTest {

	@Test
	public void testDataSource() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		
		Object result = ask(manager, new GetDataSourceStatus());
		assertEquals(TypedIterable.class, result.getClass());
		
		TypedIterable<?> typedIterable = (TypedIterable<?>)result;
		assertTrue(typedIterable.contains(DataSourceStatus.class));
		
		Iterator<DataSourceStatus> itr = typedIterable.cast(DataSourceStatus.class).iterator();
		assertNotNull(itr);
		assertTrue(itr.hasNext());
		
		DataSourceStatus status = itr.next();
		assertEquals("testDataSource", status.getDataSourceId());
		assertNull(status.getLastHarvested());
		assertNull(status.getFinishedState());
		
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
		
		result = ask(manager, new CreateHarvestJob("testDataSource"));
		assertEquals(Ack.class, result.getClass());
		
		executeJobs(new GetHarvestJobs());
		
		result = ask(manager, new GetDataSourceStatus());
		assertEquals(TypedIterable.class, result.getClass());
		
		typedIterable = (TypedIterable<?>)result;
		assertTrue(typedIterable.contains(DataSourceStatus.class));
		
		itr = typedIterable.cast(DataSourceStatus.class).iterator();
		assertNotNull(itr);
		assertTrue(itr.hasNext());
		
		status = itr.next();
		assertEquals("testDataSource", status.getDataSourceId());
		assertEquals(JobState.SUCCEEDED, status.getFinishedState());
		assertNotNull(status.getLastHarvested());
	}
	
	@Test
	public void testDataset() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		
		Dataset dataset = createTestDataset();
		Object result = ask(database, new RegisterSourceDataset("testDataSource", dataset));
		assertEquals(Registered.class, result.getClass());
		
		Table table = dataset.getTable();
		List<Column> columns = Arrays.asList(table.getColumns().get(0));
		result = ask(database, new CreateDataset(
				"testDataset", 
				"My Test Dataset", 
				dataset.getId(),
				columns,
				"{ \"expression\": null }"));
		
		result = ask(manager, new GetDatasetStatus());
		assertEquals(TypedIterable.class, result.getClass());
		
		TypedIterable<?> typedIterable = (TypedIterable<?>)result;
		assertTrue(typedIterable.contains(DatasetStatusInfo.class));
		
		Iterator<DatasetStatusInfo> itr = typedIterable.cast(DatasetStatusInfo.class).iterator();
		assertNotNull(itr);
		
		assertTrue(itr.hasNext());
		
		DatasetStatusInfo status = itr.next();
		assertEquals("testDataset", status.getDatasetId());		
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
		
		result = ask(manager, new GetDatasetStatus("testDataset"));
		assertEquals(DatasetStatusInfo.class, result.getClass());
		
		status = (DatasetStatusInfo)result;
		assertEquals("testDataset", status.getDatasetId());		
		assertNotNull(status);
		
		for(int i = 0; i < 10; i++) {
			result = ask(manager, new CreateImportJob("testDataset"));
			assertEquals(Ack.class, result.getClass());
		
			executeJobs(new GetImportJobs());
		}
		
		result = ask(manager, new GetDatasetStatus());
		assertEquals(TypedIterable.class, result.getClass());
		
		typedIterable = (TypedIterable<?>)result;
		assertTrue(typedIterable.contains(DatasetStatusInfo.class));
		
		itr = typedIterable.cast(DatasetStatusInfo.class).iterator();
		assertNotNull(itr);
		
		assertTrue(itr.hasNext());
		
		status = itr.next();
		assertEquals("testDataset", status.getDatasetId());		
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
	}
}
