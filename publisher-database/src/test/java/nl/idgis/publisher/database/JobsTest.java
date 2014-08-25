package nl.idgis.publisher.database;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QImportJobColumn.importJobColumn;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.Type;
import nl.idgis.publisher.protocol.messages.Ack;

import org.junit.Test;

import com.mysema.query.Tuple;

public class JobsTest extends AbstractDatabaseTest {	

	@Test
	public void testHarvestJob() throws Exception {
		
		insertDataSource();	
		
		Object result = ask(new CreateHarvestJob("testDataSource"));
		assertTrue(result instanceof Ack);
		
		Tuple t = query().from(job).singleResult(job.all());
		assertNotNull(t);
		assertEquals("HARVEST", t.get(job.type));
		
		result = ask(new GetHarvestJobs());		
		assertTrue(result instanceof List);
		
		List<HarvestJobInfo> jobs = (List<HarvestJobInfo>)result;
		assertFalse(jobs.isEmpty());
		
		HarvestJobInfo job = jobs.get(0);
		assertNotNull(job);
		
		assertEquals("testDataSource", job.getDataSourceId());
	}	

	private int insertDataSource() {
		return insert(dataSource)
			.set(dataSource.identification, "testDataSource")
			.set(dataSource.name, "My Test DataSource")
			.executeWithKey(dataSource.id);
	}
	
	@Test
	public void testImportJob() throws Exception {
		int dataSourceId = insertDataSource();
		
		int sourceDatasetId = 
			insert(sourceDataset)
				.set(sourceDataset.dataSourceId, dataSourceId)
				.set(sourceDataset.identification, "testSourceDataset")
				.executeWithKey(sourceDataset.id);
		
		int categoryId =
			insert(category)
				.set(category.identification, "testCategory")
				.set(category.name, "My Test Category")
				.executeWithKey(category.id);
		
		int versionId =
			insert(sourceDatasetVersion)
				.set(sourceDatasetVersion.name, "My Test SourceDataset")
				.set(sourceDatasetVersion.revision, new Timestamp(new Date().getTime()))
				.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
				.set(sourceDatasetVersion.categoryId, categoryId)
				.executeWithKey(sourceDatasetVersion.id);
		
		insert(sourceDatasetVersionColumn)
			.set(sourceDatasetVersionColumn.sourceDatasetVersionId, versionId)
			.set(sourceDatasetVersionColumn.index, 0)
			.set(sourceDatasetVersionColumn.name, "test")
			.set(sourceDatasetVersionColumn.dataType, "GEOMETRY")
			.execute();
		
		int datasetId = 
			insert(dataset)
				.set(dataset.name, "My Test Dataset")
				.set(dataset.identification, "testDataset")
				.set(dataset.sourceDatasetId, sourceDatasetId)
				.executeWithKey(dataset.id);
		
		insert(datasetColumn)
			.set(datasetColumn.datasetId, datasetId)
			.set(datasetColumn.index, 0)
			.set(datasetColumn.name, "test")
			.set(datasetColumn.dataType, "GEOMETRY")
			.execute();
		
		Object result = ask(new CreateImportJob("testDataset"));
		assertTrue(result instanceof Ack);
		
		Tuple t = query().from(job).singleResult(job.all());
		assertNotNull(t);
		assertEquals("IMPORT", t.get(job.type));
		
		t = query().from(importJobColumn).singleResult(importJobColumn.all());
		assertNotNull(t);
		assertEquals(0, t.get(importJobColumn.index).intValue());
		assertEquals("test", t.get(importJobColumn.name));
		assertEquals("GEOMETRY", t.get(importJobColumn.dataType));
		
		result = ask(new GetImportJobs());
		assertTrue(result instanceof List);
		
		List<ImportJobInfo> jobs = (List<ImportJobInfo>)result;
		assertFalse(jobs.isEmpty());
		
		ImportJobInfo job = jobs.get(0);
		assertNotNull(job);
		assertEquals("testDataset", job.getDatasetId());
		assertEquals("testCategory", job.getCategoryId());
		
		List<Column> columns = job.getColumns();
		assertNotNull(columns);
		assertFalse(columns.isEmpty());
		
		Column column = columns.get(0);
		assertNotNull(column);
		
		assertEquals("test", column.getName());
		assertEquals(Type.GEOMETRY, column.getDataType());
	}
	
}
