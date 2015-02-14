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
import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.service.manager.messages.GetGroupLayer;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.utils.TypedIterable;

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
			.set(leafLayer.datasetId, datasetId)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, layerId0)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(service)
			.set(service.identification, "service0")
			.set(service.name, "serviceName0")
			.set(service.rootgroupId, rootId) 
			.execute();
		
		Service service = sync.ask(serviceManager, new GetService("service0"), Service.class);		
		assertEquals("rootgroup", service.getRootId());
		assertEquals("serviceName0", service.getName());
		
		List<Layer> layers = service.getLayers();
		assertNotNull(layers);
		
		Iterator<Layer> itr = layers.iterator();
		assertTrue(itr.hasNext());
		
		Layer layer = itr.next();
		assertNotNull(layer);
		assertFalse(layer.isGroup());
		
		DatasetLayer datasetLayer = layer.asDataset();
		assertEquals("layer0", datasetLayer.getId());
		assertEquals("dataset0", datasetLayer.getTableName());
		
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
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, groupId)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerOrder, 0)
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
		
		List<Layer> groupLayers = group.asGroup().getLayers();
		assertNotNull(groupLayers);
		
		Iterator<Layer> groupItr = groupLayers.iterator();
		assertTrue(groupItr.hasNext());
		
		Layer layer = groupItr.next();
		assertNotNull(layer);
		assertFalse(layer.isGroup());
		
		assertFalse(groupItr.hasNext());
		
		assertFalse(rootItr.hasNext());
		
		GroupLayer groupLayer = sync.ask(serviceManager, new GetGroupLayer("group"), GroupLayer.class);
		assertEquals("group", groupLayer.getId());
		assertEquals("group-name", groupLayer.getName());
		
		groupLayers = groupLayer.getLayers();
		assertNotNull(groupLayers);
		
		groupItr = groupLayers.iterator();
		assertTrue(groupItr.hasNext());
		
		groupItr.next();
		assertNotNull(layer);
		assertFalse(layer.isGroup());
		
		assertFalse(groupItr.hasNext());
		
		assertFalse(rootItr.hasNext());
		
		TypedIterable<?> services = sync.ask(serviceManager, new GetServicesWithLayer("rootgroup"), TypedIterable.class);
		assertTrue(services.contains(String.class));
		
		Iterator<String> servicesItr = services.cast(String.class).iterator();
		assertTrue(servicesItr.hasNext());
		assertEquals("service0", servicesItr.next());
		assertFalse(servicesItr.hasNext());
		
		services = sync.ask(serviceManager, new GetServicesWithLayer("group"), TypedIterable.class);
		assertTrue(services.contains(String.class));
		
		servicesItr = services.cast(String.class).iterator();
		assertTrue(servicesItr.hasNext());
		assertEquals("service0", servicesItr.next());
		assertFalse(servicesItr.hasNext());
	}
	
	@Test
	public void testGroupInGroup() throws Exception {
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
			
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
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
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, group0Id)
			.set(layerStructure.childLayerId, group1Id)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, group1Id)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerOrder, 0)
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
		assertEquals("group0", group0.getId());
		
		List<Layer> group0Layers = group0.asGroup().getLayers();
		assertNotNull(group0Layers);
		
		Iterator<Layer> group0Itr = group0Layers.iterator();
		assertTrue(group0Itr.hasNext());
		
		Layer group1 = group0Itr.next();
		assertNotNull(group1);
		assertTrue(group1.isGroup());
		assertEquals("group1", group1.getId());
		
		List<Layer> group1Layers = group1.asGroup().getLayers();		
		assertNotNull(group1Layers);
		
		Iterator<Layer> group1Itr = group1Layers.iterator();
		assertTrue(group1Itr.hasNext());
		
		Layer layer = group1Itr.next();
		assertNotNull(layer);
		assertFalse(layer.isGroup());
		
		assertFalse(group1Itr.hasNext());
		
		assertFalse(group0Itr.hasNext());
		
		assertFalse(rootItr.hasNext());
		
		GroupLayer groupLayer = sync.ask(serviceManager, new GetGroupLayer("group0"), GroupLayer.class);
		assertEquals("group0", groupLayer.getId());
		
		group0Layers = groupLayer.getLayers();
		assertNotNull(group0Layers);
		
		group0Itr = group0Layers.iterator();
		assertTrue(group0Itr.hasNext());
		
		group1 = group0Itr.next();
		assertNotNull(group1);
		assertTrue(group1.isGroup());
		assertEquals("group1", group1.getId());
		
		group1Layers = group1.asGroup().getLayers();		
		assertNotNull(group1Layers);
		
		group1Itr = group1Layers.iterator();
		assertTrue(group1Itr.hasNext());
		
		layer = group1Itr.next();
		assertNotNull(layer);
		assertFalse(layer.isGroup());
		
		assertFalse(group1Itr.hasNext());
		
		assertFalse(group0Itr.hasNext());
		
		assertFalse(rootItr.hasNext());
	}
	
	@Test
	public void testNotFound() throws Exception {
		sync.ask(serviceManager, new GetService("service0"), NotFound.class);
	}
	
	@Test
	public void testMultipleServices() throws Exception {
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
			.set(leafLayer.datasetId, datasetId)
			.execute();
		
		int root0Id = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup0")
			.set(genericLayer.name, "rootgroup-name0")
			.executeWithKey(genericLayer.id);
		
		int root1Id = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup1")
			.set(genericLayer.name, "rootgroup-name1")
			.executeWithKey(genericLayer.id);
		
		int groupId = insert(genericLayer)
			.set(genericLayer.identification, "group")
			.set(genericLayer.name, "group-name")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, root0Id)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, root1Id)
			.set(layerStructure.childLayerId, groupId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, groupId)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(service)
			.set(service.identification, "service0")
			.set(service.rootgroupId, root0Id) 
			.execute();
		
		insert(service)
			.set(service.identification, "service1")
			.set(service.rootgroupId, root1Id) 
			.execute();
		
		Service service0 = sync.ask(serviceManager, new GetService("service0"), Service.class);
		
		List<Layer> service0Layers = service0.getLayers();
		assertNotNull(service0Layers);
		
		Iterator<Layer> service0Itr = service0Layers.iterator();
		assertTrue(service0Itr.hasNext());
		
		Layer service0Layer = service0Itr.next();
		assertNotNull(service0Layer);
		assertFalse(service0Layer.isGroup());
		assertEquals("layer", service0Layer.getId());
		
		assertFalse(service0Itr.hasNext());
		
		Service service1 = sync.ask(serviceManager, new GetService("service1"), Service.class);
		
		List<Layer> service1Layers = service1.getLayers();
		assertNotNull(service1Layers);
		
		Iterator<Layer> service1Itr = service1Layers.iterator();
		assertTrue(service1Itr.hasNext());
		
		Layer group = service1Itr.next();
		assertNotNull(group);
		assertTrue(group.isGroup());
		
		List<Layer> groupLayers = group.asGroup().getLayers();
		assertNotNull(groupLayers);
		
		Iterator<Layer> groupItr = groupLayers.iterator();
		assertTrue(groupItr.hasNext());
		
		Layer service1Layer = groupItr.next();
		assertNotNull(service1Layer);
		assertFalse(service1Layer.isGroup());
		assertEquals("layer", service1Layer.getId());
		
		assertFalse(groupItr.hasNext());
		
		assertFalse(service1Itr.hasNext());
	}
	
	@Test
	public void testLayerOrder() throws Exception {
		List<Integer> layerIds = new ArrayList<>();
		List<String> layerIdentifications = new ArrayList<>();
		
		for(int i = 0; i < 10; i++) {
			String layerIdentification = UUID.randomUUID().toString();
			
			layerIdentifications.add(layerIdentification);
			
			int layer = insert(genericLayer)
				.set(genericLayer.identification, layerIdentification)
				.set(genericLayer.name,  layerIdentification + "-name")
				.executeWithKey(genericLayer.id);
			
			insert(leafLayer)
				.set(leafLayer.genericLayerId, layer)
				.set(leafLayer.datasetId, datasetId)
				.execute();
			
			layerIds.add(layer);
		}
		
		int rootId = insert(genericLayer)
				.set(genericLayer.identification, "rootgroup")
				.set(genericLayer.name, "rootgroup-name")
				.executeWithKey(genericLayer.id);
			
		int order = 0;
		for(int layerId : layerIds) {
			insert(layerStructure)
				.set(layerStructure.parentLayerId, rootId)
				.set(layerStructure.childLayerId, layerId)
				.set(layerStructure.layerOrder, order++)
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
		
		Iterator<Layer> layersItr = layers.iterator();
		for(String layerIdentification : layerIdentifications) {
			assertTrue(layersItr.hasNext());
			
			Layer layer = layersItr.next();
			assertNotNull(layer);
			assertEquals(layerIdentification, layer.getId());
		}
		
		assertFalse(layersItr.hasNext());
	}
}
