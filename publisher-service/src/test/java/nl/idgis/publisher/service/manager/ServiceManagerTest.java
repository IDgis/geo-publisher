package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QImportJobColumn.importJobColumn;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.mysema.query.sql.SQLSubQuery;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.service.manager.messages.DatasetLayer;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.Layer;
import nl.idgis.publisher.service.manager.messages.Service;

public class ServiceManagerTest extends AbstractServiceTest {
		
	@Test
	public void testGetService() throws Exception {
		int dataSourceId = insert(dataSource)
			.set(dataSource.identification, "datasource0")
			.set(dataSource.name, "datasource-name0")
			.executeWithKey(dataSource.id);
		
		int categoryId = insert(category)
			.set(category.identification, "category0")
			.set(category.name, "category-name0")
			.executeWithKey(category.id);
		
		int sourceDatasetId = insert(sourceDataset)
			.set(sourceDataset.identification, "sourcedataset0")
			.set(sourceDataset.dataSourceId, dataSourceId)
			.executeWithKey(sourceDataset.id);
		
		int sourceDatasetVersionId = insert(sourceDatasetVersion)
			.set(sourceDatasetVersion.sourceDatasetId, sourceDatasetId)
			.set(sourceDatasetVersion.categoryId, categoryId)
			.set(sourceDatasetVersion.name, "sourcedataset-name0")
			.set(sourceDatasetVersion.type, "VECTOR")
			.executeWithKey(sourceDatasetVersion.id);
		
		for(int i = 0; i < 10; i++) {
			insert(sourceDatasetVersionColumn)
			.set(sourceDatasetVersionColumn.sourceDatasetVersionId, sourceDatasetVersionId)
			.set(sourceDatasetVersionColumn.index, i)
			.set(sourceDatasetVersionColumn.name, "column" + i)
			.set(sourceDatasetVersionColumn.dataType, "TEXT")
			.execute();
		}
		
		int datasetId = insert(dataset)
			.set(dataset.identification, "dataset0")
			.set(dataset.name, "dataset-name0")
			.set(dataset.fileUuid, UUID.randomUUID().toString())
			.set(dataset.uuid, UUID.randomUUID().toString())
			.set(dataset.sourceDatasetId, sourceDatasetId)
			.executeWithKey(dataset.id);
		
		insert(datasetColumn)
			.columns(
				datasetColumn.datasetId,
				datasetColumn.index,
				datasetColumn.name,
				datasetColumn.dataType)
			.select(new SQLSubQuery().from(sourceDatasetVersionColumn)
				.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(sourceDatasetVersionId))
				.list(
					datasetId,
					sourceDatasetVersionColumn.index,
					sourceDatasetVersionColumn.name,
					sourceDatasetVersionColumn.dataType))
			.execute();
		
		int jobId = insert(job)
			.set(job.type, "IMPORT")
			.executeWithKey(job.id);
		
		int importJobId = insert(importJob)
			.set(importJob.jobId, jobId)
			.set(importJob.sourceDatasetVersionId, sourceDatasetVersionId)
			.set(importJob.datasetId, datasetId)
			.set(importJob.filterConditions, "{ \"expression\": null }")
			.executeWithKey(importJob.id);
		
		insert(importJobColumn)
			.columns(
				importJobColumn.importJobId,
				importJobColumn.index,
				importJobColumn.name,
				importJobColumn.dataType)
			.select(new SQLSubQuery().from(datasetColumn)
				.where(datasetColumn.datasetId.eq(datasetId))
				.list(
					importJobId,
					datasetColumn.index,
					datasetColumn.name,
					datasetColumn.dataType))
			.execute();
		
		int genericLayerId = insert(genericLayer)
			.set(genericLayer.identification, "layer0")
			.set(genericLayer.name, "layer-name0")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, genericLayerId)
			.set(leafLayer.identification, "layer0") // TODO: remove column
			.set(leafLayer.datasetId, datasetId)
			.execute();
		
		int rootGroupId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootGroupId)
			.set(layerStructure.childLayerId, genericLayerId)
			.set(layerStructure.layerorder, 0)
			.execute();
		
		insert(service)
			.set(service.identification, "service0")
			.set(service.rootgroupId, rootGroupId) 
			.execute();
		
		Service service = sync.ask(serviceManager, new GetService("service0"), Service.class);		
		assertEquals("rootgroup", service.getRootId());
		
		List<Layer> layers = service.getLayers();
		assertNotNull(layers);
		
		Iterator<Layer> itr = layers.iterator();
		assertTrue(itr.hasNext());
		
		Layer layer = itr.next();
		assertNotNull(layer);
		assertTrue(layer.isDataset());
		
		DatasetLayer datasetLayer = layer.asDataset();
		assertEquals("layer0", datasetLayer.getId());
		
		assertFalse(itr.hasNext());
	}
}
