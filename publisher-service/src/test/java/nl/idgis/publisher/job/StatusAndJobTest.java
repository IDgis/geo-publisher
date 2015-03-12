package nl.idgis.publisher.job;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import nl.idgis.publisher.AbstractServiceTest;

import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetStatus;

import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.domain.service.Table;

import nl.idgis.publisher.job.manager.messages.CreateHarvestJob;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.GetHarvestJobs;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedList;

import org.junit.Test;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StatusAndJobTest extends AbstractServiceTest {

	@Test
	public void testDataSource() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		
		Object result = f.ask(database, new GetDataSourceStatus()).get();
		assertEquals(TypedList.class, result.getClass());
		
		TypedList<?> typedList = (TypedList<?>)result;
		assertTrue(typedList.contains(DataSourceStatus.class));
		
		Iterator<DataSourceStatus> itr = typedList.cast(DataSourceStatus.class).iterator();
		assertNotNull(itr);
		assertTrue(itr.hasNext());
		
		DataSourceStatus status = itr.next();
		assertEquals("testDataSource", status.getDataSourceId());
		assertNull(status.getLastHarvested());
		assertNull(status.getFinishedState());
		
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
		
		result = f.ask(jobManager, new CreateHarvestJob("testDataSource")).get();
		assertEquals(Ack.class, result.getClass());
		
		executeJobs(new GetHarvestJobs());
		
		result = f.ask(database, new GetDataSourceStatus()).get();
		assertEquals(TypedList.class, result.getClass());
		
		typedList = (TypedList<?>)result;
		assertTrue(typedList.contains(DataSourceStatus.class));
		
		itr = typedList.cast(DataSourceStatus.class).iterator();
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
		
		VectorDataset dataset = createVectorDataset();
		Object result = f.ask(datasetManager, new RegisterSourceDataset("testDataSource", dataset)).get();
		assertEquals(Registered.class, result.getClass());
		
		Table table = dataset.getTable();
		List<Column> columns = Arrays.asList(table.getColumns().get(0));
		result = f.ask(database, new CreateDataset(
				"testDataset", 
				"My Test Dataset", 
				dataset.getId(),
				columns,
				"{ \"expression\": null }")).get();
		
		result = f.ask(database, new GetDatasetStatus()).get();
		assertEquals(TypedList.class, result.getClass());
		
		TypedList<?> typedList = (TypedList<?>)result;
		assertTrue(typedList.contains(DatasetStatusInfo.class));
		
		Iterator<DatasetStatusInfo> itr = typedList.cast(DatasetStatusInfo.class).iterator();
		assertNotNull(itr);
		
		assertTrue(itr.hasNext());
		
		DatasetStatusInfo status = itr.next();
		assertEquals("testDataset", status.getDatasetId());		
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
		
		result = f.ask(database, new GetDatasetStatus("testDataset")).get();
		assertEquals(DatasetStatusInfo.class, result.getClass());
		
		status = (DatasetStatusInfo)result;
		assertEquals("testDataset", status.getDatasetId());		
		assertNotNull(status);
		
		for(int i = 0; i < 10; i++) {
			result = f.ask(jobManager, new CreateImportJob("testDataset")).get();
			assertEquals(Ack.class, result.getClass());
		
			executeJobs(new GetImportJobs());
		}
		
		result = f.ask(database, new GetDatasetStatus()).get();
		assertEquals(TypedList.class, result.getClass());
		
		typedList = (TypedList<?>)result;
		assertTrue(typedList.contains(DatasetStatusInfo.class));
		
		itr = typedList.cast(DatasetStatusInfo.class).iterator();
		assertNotNull(itr);
		
		assertTrue(itr.hasNext());
		
		status = itr.next();
		assertEquals("testDataset", status.getDatasetId());		
		assertNotNull(status);
		
		assertFalse(itr.hasNext());
	}
}
