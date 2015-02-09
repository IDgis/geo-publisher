package nl.idgis.publisher.service.manager.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class DefaultServiceTest {

	@Test
	public void testRootLayers() {
		List<Dataset> datasets = Arrays.asList(
			new Dataset("leaf0", "mySchema0", "myTable0"),
			new Dataset("leaf1", "mySchema1", "myTable1"),
			new Dataset("leaf2", "mySchema2", "myTable2"));
		
		Map<String, String> groups = new LinkedHashMap<>();
		groups.put("leaf0", null);
		groups.put("leaf1", null);
		groups.put("leaf2", null);
		
		Service service = new DefaultService("service0", datasets, groups);
		List<Layer> layers = service.getLayers();
		assertNotNull(layers);
		
		Iterator<Layer> itr = layers.iterator();
		assertDatasetLayer(itr, "leaf0", "mySchema0", "myTable0");
		assertDatasetLayer(itr, "leaf1", "mySchema1", "myTable1");
		assertDatasetLayer(itr, "leaf2", "mySchema2", "myTable2");
		assertFalse(itr.hasNext());	
	}
	
	@Test
	public void testGroupLayer() {
		List<Dataset> datasets = Arrays.asList(
				new Dataset("leaf0", "mySchema0", "myTable0"),
				new Dataset("leaf1", "mySchema1", "myTable1"),
				new Dataset("leaf2", "mySchema2", "myTable2"));
			
		Map<String, String> groups = new LinkedHashMap<>();
		groups.put("group0", null);
		groups.put("leaf0", "group0");
		groups.put("leaf1", "group0");
		groups.put("leaf2", "group0");
		
		Service service = new DefaultService("service0", datasets, groups);
		List<Layer> layers = service.getLayers();
		assertNotNull(layers);
		
		Iterator<Layer> itr = layers.iterator();
		List<Layer> childLayers = assertGroupLayer(itr, "group0").getLayers();
		assertNotNull(childLayers);
		
		Iterator<Layer> childItr = childLayers.iterator();
		assertDatasetLayer(childItr, "leaf0", "mySchema0", "myTable0");
		assertDatasetLayer(childItr, "leaf1", "mySchema1", "myTable1");
		assertDatasetLayer(childItr, "leaf2", "mySchema2", "myTable2");
		
		assertFalse(childItr.hasNext());
	}
	
	private GroupLayer assertGroupLayer(Iterator<Layer> itr, String id) {
		assertTrue(itr.hasNext());
		
		Layer layer = itr.next();
		assertNotNull(layer);
		assertEquals(id, layer.getId());
		assertTrue(layer.isGroup());
		assertFalse(layer.isDataset());
		
		return layer.asGroup();
	}
	
	private void assertDatasetLayer(Iterator<Layer> itr, String id, String schemaName, String tableName) {
		assertTrue(itr.hasNext());
		
		Layer layer = itr.next();
		assertNotNull(layer);
		assertTrue(layer.isDataset());
		assertFalse(layer.isGroup());
		
		DatasetLayer datasetLayer = layer.asDataset();
		assertEquals(id, datasetLayer.getId());
		assertEquals(schemaName, datasetLayer.getSchemaName());
		assertEquals(tableName, datasetLayer.getTableName());
	}
}
