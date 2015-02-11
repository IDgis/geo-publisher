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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.mysema.query.sql.SQLSubQuery;

import nl.idgis.publisher.domain.web.NotFound;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.service.manager.messages.DatasetLayer;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.Layer;
import nl.idgis.publisher.service.manager.messages.Service;

public class ServiceManagerTest extends AbstractServiceTest {
	
	int datasetId;
		
	@Before
	public void databaseContent() throws Exception {
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
		
		datasetId = insert(dataset)
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
	}
		
	@Test
	public void testSingleLayer() throws Exception {
		int layerId0 = insert(genericLayer)
			.set(genericLayer.identification, "layer0")
			.set(genericLayer.name, "layer-name0")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId0)
			.set(leafLayer.identification, "layer0") // TODO: remove column
			.set(leafLayer.datasetId, datasetId)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, layerId0)
			.set(layerStructure.layerorder, 0)
			.execute();
		
		insert(service)
			.set(service.identification, "service0")
			.set(service.rootgroupId, rootId) 
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
	
	@Test
	public void testMultipleLayers() throws Exception {
		List<Integer> layerIds = new ArrayList<>();
		
		for(int i = 0; i < 10; i++) {		
			int layer = insert(genericLayer)
				.set(genericLayer.identification, "layer" + i)
				.set(genericLayer.name, "layer-name0")
				.executeWithKey(genericLayer.id);
			
			insert(leafLayer)
				.set(leafLayer.genericLayerId, layer)
				.set(leafLayer.identification, "layer" + i) // TODO: remove column
				.set(leafLayer.datasetId, datasetId)
				.execute();
			
			layerIds.add(layer);
		}
		
		int rootId = insert(genericLayer)
				.set(genericLayer.identification, "rootgroup")
				.set(genericLayer.name, "rootgroup-name")
				.executeWithKey(genericLayer.id);
			
		for(int layerId : layerIds) {
			insert(layerStructure)
				.set(layerStructure.parentLayerId, rootId)
				.set(layerStructure.childLayerId, layerId)
				.set(layerStructure.layerorder, 0)
				.execute();
		}
		
		insert(service)
			.set(service.identification, "service0")
			.set(service.rootgroupId, rootId) 
			.execute();
	
		Service service = sync.ask(serviceManager, new GetService("service0"), Service.class);		
		assertEquals("rootgroup", service.getRootId());
		
		List<Layer> layers = service.getLayers();
		assertNotNull(layers);
		
		Iterator<Layer> itr = layers.iterator();
		for(int i = 0 ; i < layerIds.size(); i++) {
			assertTrue(itr.hasNext());
			
			Layer layer = itr.next();
			assertEquals("layer" + i, layer.getId());
		}
		
		assertFalse(itr.hasNext());
	}
	
	@Test
	public void testGroup() throws Exception {
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
			.set(leafLayer.identification, "layer") // TODO: remove column
			.set(leafLayer.datasetId, datasetId)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.executeWithKey(genericLayer.id);
		
		int groupId = insert(genericLayer)
			.set(genericLayer.identification, "group")
			.set(genericLayer.name, "group-name")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, groupId)
			.set(layerStructure.layerorder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, groupId)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerorder, 0)
			.execute();
		
		insert(service)
			.set(service.identification, "service0")
			.set(service.rootgroupId, rootId) 
			.execute();
		
		Service service = sync.ask(serviceManager, new GetService("service0"), Service.class);
		
		List<Layer> rootLayers = service.getLayers();
		assertNotNull(rootLayers);
		
		Iterator<Layer> rootItr = rootLayers.iterator();
		assertTrue(rootItr.hasNext());
		
		Layer group = rootItr.next();
		assertNotNull(group);
		assertTrue(group.isGroup());
		assertFalse(group.isDataset());
		
		List<Layer> groupLayers = group.asGroup().getLayers();
		assertNotNull(groupLayers);
		
		Iterator<Layer> groupItr = groupLayers.iterator();
		assertTrue(groupItr.hasNext());
		
		Layer layer = groupItr.next();
		assertNotNull(layer);
		assertFalse(layer.isGroup());
		assertTrue(layer.isDataset());
		
		assertFalse(groupItr.hasNext());
		
		assertFalse(rootItr.hasNext());
	}
	
	@Test
	public void testGroupInGroup() throws Exception {
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
			
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
			.set(leafLayer.identification, "layer") // TODO: remove column
			.set(leafLayer.datasetId, datasetId)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.executeWithKey(genericLayer.id);
		
		int group0Id = insert(genericLayer)
			.set(genericLayer.identification, "group0")
			.set(genericLayer.name, "group-name0")
			.executeWithKey(genericLayer.id);
		
		int group1Id = insert(genericLayer)
			.set(genericLayer.identification, "group1")
			.set(genericLayer.name, "group-name1")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, group0Id)
			.set(layerStructure.layerorder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, group0Id)
			.set(layerStructure.childLayerId, group1Id)
			.set(layerStructure.layerorder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, group1Id)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerorder, 0)
			.execute();
		
		insert(service)
			.set(service.identification, "service0")
			.set(service.rootgroupId, rootId) 
			.execute();
		
		Service service = sync.ask(serviceManager, new GetService("service0"), Service.class);
		
		List<Layer> rootLayers = service.getLayers();
		assertNotNull(rootLayers);
		
		Iterator<Layer> rootItr = rootLayers.iterator();
		assertTrue(rootItr.hasNext());
		
		Layer group0 = rootItr.next();
		assertNotNull(group0);
		assertTrue(group0.isGroup());
		assertFalse(group0.isDataset());
		assertEquals("group0", group0.getId());
		
		List<Layer> group0Layers = group0.asGroup().getLayers();
		assertNotNull(group0Layers);
		
		Iterator<Layer> group0Itr = group0Layers.iterator();
		assertTrue(group0Itr.hasNext());
		
		Layer group1 = group0Itr.next();
		assertNotNull(group1);
		assertTrue(group1.isGroup());
		assertFalse(group1.isDataset());
		assertEquals("group1", group1.getId());
		
		List<Layer> group1Layers = group1.asGroup().getLayers();		
		assertNotNull(group1Layers);
		
		Iterator<Layer> group1Itr = group1Layers.iterator();
		assertTrue(group1Itr.hasNext());
		
		Layer layer = group1Itr.next();
		assertNotNull(layer);
		assertFalse(layer.isGroup());
		assertTrue(layer.isDataset());
		
		assertFalse(group1Itr.hasNext());
		
		assertFalse(group0Itr.hasNext());
		
		assertFalse(rootItr.hasNext());
	}
	
	@Test
	public void testNotFound() throws Exception {
		sync.ask(serviceManager, new GetService("service0"), NotFound.class);
	}
}
