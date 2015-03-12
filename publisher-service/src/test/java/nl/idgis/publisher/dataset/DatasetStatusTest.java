package nl.idgis.publisher.dataset;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import nl.idgis.publisher.AbstractServiceTest;

import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.UpdateDataset;

import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.VectorDataset;
import nl.idgis.publisher.domain.service.Table;

import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;

import org.junit.Before;
import org.junit.Test;

public class DatasetStatusTest extends AbstractServiceTest {
	
	VectorDataset testDataset;
	Table testTable;

	@Before
	public void databaseContent() throws Exception {
		insertDataSource();
	
		testDataset = createVectorDataset();
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", testDataset)).get();
		
		testTable = testDataset.getTable();
		f.ask(database, new CreateDataset("testDataset", "My Test Dataset", testDataset.getId(), testTable.getColumns(), "")).get();
	}	

	@Test
	public void testImported() throws Exception {
		// initially a dataset is not imported		
		assertFalse(
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get()
			
			.isImported());		
		
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		
		// import job created, still not imported
		assertFalse(
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get()
			
			.isImported());
		
		executeJobs(new GetImportJobs());
		
		// import job executed -> imported
		assertTrue(
			f.ask(database, 
				new GetDatasetStatus("testDataset"),
				DatasetStatusInfo.class).get()
				
			.isImported());
		
		f.ask(jobManager, new CreateImportJob("testDataset")).get();
		
		// a new import job created, but dataset is still imported
		assertTrue(
			f.ask(database, 
				new GetDatasetStatus("testDataset"),
				DatasetStatusInfo.class).get()
				
			.isImported());
		
		executeJobs(new GetImportJobs());
		
		assertTrue(
			f.ask(database, 
				new GetDatasetStatus("testDataset"),
				DatasetStatusInfo.class).get()
				
			.isImported());
	}
	
	@Test
	public void testDatasetChanged() throws Exception {
		// not yet imported, changes are calculated between currently configured 
		// dataset and last imported configuration		
		DatasetStatusInfo status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
		
		f.ask(jobManager, new CreateImportJob("testDataset")).get();		
		
		// import job created, still not imported
		status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
		
		executeJobs(new GetImportJobs());
		
		// import job executed -> imported, but still not changes
		status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
			
		// remove second column
		List<Column> newColumns = Arrays.asList(testTable.getColumns().get(0));
		f.ask(database, new UpdateDataset(
			"testDataset",
			"My Test Dataset",
			"testSourceDataset",
			newColumns, "")).get();
		
		status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertTrue(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());

		// change source dataset 
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", createVectorDataset("newSourceDataset"))).get();
		f.ask(database, new UpdateDataset(
				"testDataset",
				"My Test Dataset",
				"newSourceDataset",
				newColumns, "")).get();
		
		status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertTrue(status.isColumnsChanged());
		assertTrue(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
		
		// change filter condition 
		f.ask(datasetManager, new RegisterSourceDataset("testDataSource", createVectorDataset("newSourceDataset"))).get();
		f.ask(database, new UpdateDataset(
				"testDataset",
				"My Test Dataset",
				"newSourceDataset",
				newColumns, "fakeFilterCondition")).get();
		
		status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertTrue(status.isColumnsChanged());
		assertTrue(status.isSourceDatasetChanged());
		assertTrue(status.isFilterConditionChanged());
		
		f.ask(jobManager, new CreateImportJob("testDataset")).get();		
		
		// import job created, no changes yet
		status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertTrue(status.isColumnsChanged());
		assertTrue(status.isSourceDatasetChanged());
		assertTrue(status.isFilterConditionChanged());
		
		executeJobs(new GetImportJobs());
		
		// dataset updated
		status = 
			f.ask(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class).get();
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
	}
	
	@Test
	public void testSourceDatasetChanged() throws Exception {
		// TODO
	}
}
