package nl.idgis.publisher.database;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.database.messages.DatasetStatus;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.UpdateJobState;
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

public class StatusAndJobTest extends AbstractDatabaseTest {
	
	private void executeJob(Object msg) throws Exception {
		Object result = ask(msg);
		assertTrue(result instanceof List);	
		
		List<?> list = (List<?>)result;
		
		Iterator<?> listItr = list.iterator();
		assertNotNull(listItr);
		assertTrue(listItr.hasNext());
		
		Object listItem = listItr.next();
		assertTrue(listItem instanceof JobInfo);
		
		JobInfo jobInfo = (JobInfo)listItem;		
		ask(new UpdateJobState(jobInfo, JobState.SUCCEEDED));
		
		assertNotNull(listItem);
		
		assertFalse(listItr.hasNext());
	}

	@Test
	public void testDataSource() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		
		Object result = ask(new GetDataSourceStatus());
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
		
		result = ask(new CreateHarvestJob("testDataSource"));
		assertEquals(Ack.class, result.getClass());
		
		executeJob(new GetHarvestJobs());
		
		result = ask(new GetDataSourceStatus());
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
		Object result = ask(new RegisterSourceDataset("testDataSource", dataset));
		assertEquals(Registered.class, result.getClass());
		
		Table table = dataset.getTable();
		List<Column> columns = Arrays.asList(table.getColumns().get(0));
		result = ask(new CreateDataset(
				"testDataset", 
				"My Test Dataset", 
				dataset.getId(),
				columns,
				"{ \"expression\": null }"));
		
		result = ask(new GetDatasetStatus());
		assertEquals(TypedIterable.class, result.getClass());
		
		TypedIterable<?> typedIterable = (TypedIterable<?>)result;
		assertTrue(typedIterable.contains(DatasetStatus.class));
		
		Iterator<DatasetStatus> itr = typedIterable.cast(DatasetStatus.class).iterator();
		assertNotNull(itr);
		
		assertTrue(itr.hasNext());
		
		DatasetStatus status = itr.next();
		assertEquals("testDataset", status.getDatasetId());
		assertEquals(columns, status.getColumns());
		assertNull(status.getImportedColumns());
		assertNull(status.getImportedSourceColumns());
		assertNull(status.getImportedSourceDatasetId());
		assertEquals(table.getColumns(), status.getSourceColumns());
		assertEquals(dataset.getId(), status.getSourceDatasetId());
		assertEquals(dataset.getRevisionDate().getTime(), status.getSourceRevision().getTime());
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
		
		for(int i = 0; i < 10; i++) {
			result = ask(new CreateImportJob("testDataset"));
			assertEquals(Ack.class, result.getClass());
		
			executeJob(new GetImportJobs());
		}
		
		result = ask(new GetDatasetStatus());
		assertEquals(TypedIterable.class, result.getClass());
		
		typedIterable = (TypedIterable<?>)result;
		assertTrue(typedIterable.contains(DatasetStatus.class));
		
		itr = typedIterable.cast(DatasetStatus.class).iterator();
		assertNotNull(itr);
		
		assertTrue(itr.hasNext());
		
		status = itr.next();
		assertEquals("testDataset", status.getDatasetId());
		assertEquals(columns, status.getColumns());
		assertEquals(columns, status.getImportedColumns());
		assertEquals(table.getColumns(), status.getImportedSourceColumns());		
		assertEquals("testSourceDataset", status.getImportedSourceDatasetId());
		assertEquals(table.getColumns(), status.getSourceColumns());
		assertEquals(dataset.getId(), status.getSourceDatasetId());
		assertEquals(dataset.getRevisionDate().getTime(), status.getSourceRevision().getTime());
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
	}
}
