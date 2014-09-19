package nl.idgis.publisher.database;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import nl.idgis.publisher.database.messages.CreateDataset;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetServiceJobs;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.UpdateDataset;

import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.service.Table;

import org.junit.Before;
import org.junit.Test;

import static nl.idgis.publisher.utils.TestPatterns.ask;
import static nl.idgis.publisher.utils.TestPatterns.askAssert;

public class DatasetStatusTest extends AbstractDatabaseTest {
	
	Dataset testDataset;
	Table testTable;

	@Before
	public void databaseContent() throws Exception {
		insertDataSource();
	
		testDataset = createTestDataset();
		ask(database, new RegisterSourceDataset("testDataSource", testDataset));
		
		testTable = testDataset.getTable();
		ask(database, new CreateDataset("testDataset", "My Test Dataset", testDataset.getId(), testTable.getColumns(), ""));
	}	

	@Test
	public void testImported() throws Exception {
		// initially a dataset is not imported		
		assertFalse(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isImported());		
		
		ask(database, new CreateImportJob("testDataset"));
		
		// import job created, still not imported
		assertFalse(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isImported());
		
		executeJobs(new GetImportJobs());
		
		// import job executed -> imported
		assertTrue(
			askAssert(database, 
				new GetDatasetStatus("testDataset"),
				DatasetStatusInfo.class)
				
			.isImported());
		
		ask(database, new CreateImportJob("testDataset"));
		
		// a new import job created, but dataset is still imported
		assertTrue(
			askAssert(database, 
				new GetDatasetStatus("testDataset"),
				DatasetStatusInfo.class)
				
			.isImported());
		
		executeJobs(new GetImportJobs());
		
		assertTrue(
			askAssert(database, 
				new GetDatasetStatus("testDataset"),
				DatasetStatusInfo.class)
				
			.isImported());
	}
	
	@Test
	public void testServiceCreated() throws Exception {
		// initially a service is not yet created for a dataset
		assertFalse(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isServiceCreated());
		
		ask(database, new CreateImportJob("testDataset"));
		
		executeJobs(new GetImportJobs());
		
		// importing a dataset doesn't create a service
		assertFalse(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isServiceCreated());
		
		ask(database, new CreateServiceJob("testDataset"));
		
		// service job created, service still not created
		assertFalse(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isServiceCreated());
		
		executeJobs(new GetServiceJobs());
		
		// service job executed -> service created
		assertTrue(
				askAssert(database, 
					new GetDatasetStatus("testDataset"), 
					DatasetStatusInfo.class)
				
				.isServiceCreated());
		
		ask(database, new CreateImportJob("testDataset"));
		
		// import job created, existing service still created
		assertTrue(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isServiceCreated());
		
		executeJobs(new GetImportJobs());
		
		// import job could(!) have introduced a new table -> service maybe(!) not created 
		assertFalse(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isServiceCreated());
		
		ask(database, new CreateServiceJob("testDataset"));
		
		// service job created, service still not created
		assertFalse(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isServiceCreated());
		
		executeJobs(new GetServiceJobs());
		
		// service job executed -> service created
		assertTrue(
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class)
			
			.isServiceCreated());
	}
	
	@Test
	public void testDatasetChanged() throws Exception {
		// not yet imported, changes are calculated between currently configured 
		// dataset and last imported configuration		
		DatasetStatusInfo status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
		
		ask(database, new CreateImportJob("testDataset"));		
		
		// import job created, still not imported
		status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
		
		executeJobs(new GetImportJobs());
		
		// import job executed -> imported, but still not changes
		status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
			
		// remove second column
		List<Column> newColumns = Arrays.asList(testTable.getColumns().get(0));
		ask(database, new UpdateDataset(
			"testDataset",
			"My Test Dataset",
			"testSourceDataset",
			newColumns, ""));
		
		status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertTrue(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());

		// change source dataset 
		ask(database, new RegisterSourceDataset("testDataSource", createTestDataset("newSourceDataset")));
		ask(database, new UpdateDataset(
				"testDataset",
				"My Test Dataset",
				"newSourceDataset",
				newColumns, ""));
		
		status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertTrue(status.isColumnsChanged());
		assertTrue(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
		
		// change filter condition 
		ask(database, new RegisterSourceDataset("testDataSource", createTestDataset("newSourceDataset")));
		ask(database, new UpdateDataset(
				"testDataset",
				"My Test Dataset",
				"newSourceDataset",
				newColumns, "fakeFilterCondition"));
		
		status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertTrue(status.isColumnsChanged());
		assertTrue(status.isSourceDatasetChanged());
		assertTrue(status.isFilterConditionChanged());
		
		ask(database, new CreateImportJob("testDataset"));		
		
		// import job created, no changes yet
		status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertTrue(status.isColumnsChanged());
		assertTrue(status.isSourceDatasetChanged());
		assertTrue(status.isFilterConditionChanged());
		
		executeJobs(new GetImportJobs());
		
		// dataset updated
		status = 
			askAssert(database, 
				new GetDatasetStatus("testDataset"), 
				DatasetStatusInfo.class);
		
		assertFalse(status.isColumnsChanged());
		assertFalse(status.isSourceDatasetChanged());
		assertFalse(status.isFilterConditionChanged());
	}
	
	@Test
	public void testSourceDatasetChanged() throws Exception {
		// TODO
	}
}
