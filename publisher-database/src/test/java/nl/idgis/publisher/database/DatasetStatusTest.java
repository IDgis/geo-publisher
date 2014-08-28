package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.database.messages.Updated;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;

import org.junit.Test;

public class DatasetStatusTest extends AbstractDatabaseTest {

	@Test
	public void testStatus() throws Exception {
		insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.execute();
		
		Dataset d = createTestDataset();
		ask(new RegisterSourceDataset("testDataSource", d));
		
		Table t = d.getTable();
		ask(new CreateDataset("testDataset", "My Test Dataset", d.getId(), t.getColumns(), ""));
		
		assertNull(
			query().from(dataset)
				.leftJoin(lastImportJob).on(lastImportJob.datasetId.eq(dataset.id))
				.singleResult(lastImportJob.jobId));
				
		Object result = ask(new GetDatasetStatus("testDataset"));
		assertEquals(DatasetStatusInfo.class, result.getClass());
		
		DatasetStatusInfo status = (DatasetStatusInfo)result;
		assertFalse(status.isImported());
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isServiceCreated());
		assertFalse(status.isSourceDatasetColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isSourceDatasetRevisionChanged());
		
		ask(new CreateImportJob("testDataset"));
		
		assertNull(
			query().from(dataset)
				.leftJoin(lastImportJob).on(lastImportJob.datasetId.eq(dataset.id))
				.singleResult(lastImportJob.jobId));
		
		result = ask(new GetDatasetStatus("testDataset"));
		assertEquals(DatasetStatusInfo.class, result.getClass());
		
		status = (DatasetStatusInfo)result;
		assertFalse(status.isImported());
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isServiceCreated());
		assertFalse(status.isSourceDatasetColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isSourceDatasetRevisionChanged());
		 
		for(JobInfo job : (List<JobInfo>)ask(new GetImportJobs())) {
			ask(new UpdateJobState(job, JobState.SUCCEEDED));
			
			assertEquals(
				job.getId(),
					
				query().from(dataset)
					.leftJoin(lastImportJob).on(lastImportJob.datasetId.eq(dataset.id))
					.singleResult(lastImportJob.jobId).intValue());
		}
		
		
		result = ask(new GetDatasetStatus("testDataset"));
		assertEquals(DatasetStatusInfo.class, result.getClass());
		
		status = (DatasetStatusInfo)result;
		assertTrue(status.isImported());
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isServiceCreated());
		assertFalse(status.isSourceDatasetColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isSourceDatasetRevisionChanged());
		
		t = new Table(
				t.getName(),
				Arrays.asList(t.getColumns().get(0)));
		
		d = new Dataset(
			d.getId(),
			d.getCategoryId(),
			t,
			d.getRevisionDate());
		
		assertEquals(
				Updated.class,
				
				ask(new RegisterSourceDataset("testDataSource", d)).getClass());
		
	
		result = ask(new GetDatasetStatus("testDataset"));
		assertEquals(DatasetStatusInfo.class, result.getClass());
		
		status = (DatasetStatusInfo)result;
		assertTrue(status.isImported());
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isServiceCreated());
		assertTrue(status.isSourceDatasetColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isSourceDatasetRevisionChanged());
		
		ask(new CreateImportJob("testDataset"));
		
		result = ask(new GetDatasetStatus("testDataset"));
		assertEquals(DatasetStatusInfo.class, result.getClass());
		
		status = (DatasetStatusInfo)result;
		assertTrue(status.isImported());
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isServiceCreated());
		assertTrue(status.isSourceDatasetColumnsChanged());  
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isSourceDatasetRevisionChanged());
	}
}
