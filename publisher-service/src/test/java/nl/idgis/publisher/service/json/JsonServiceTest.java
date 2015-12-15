package nl.idgis.publisher.service.json;

import static java.util.Collections.emptyList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.GroupLayerRef;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

import nl.idgis.publisher.service.json.JsonService;

public class JsonServiceTest {
	
	private Service createEmptyService() {		
		Service service = mock(Service.class);
		
		when(service.getId()).thenReturn("service-id");
		when(service.getName()).thenReturn("service-name");
		when(service.getTitle()).thenReturn("service-title");
		when(service.getAbstract()).thenReturn("service-abstract");
		when(service.getKeywords()).thenReturn(Arrays.asList(
			"keyword0",
			"keyword1"));
		when(service.getContact()).thenReturn("service-contact");
		when(service.getOrganization()).thenReturn("service-organization");
		when(service.getPosition()).thenReturn("service-position");
		when(service.getAddressType()).thenReturn("service-address-type");
		when(service.getAddress()).thenReturn("service-address");
		when(service.getCity()).thenReturn("service-city");
		when(service.getZipcode()).thenReturn("service-zipcode");
		when(service.getCountry()).thenReturn("service-country");
		when(service.getTelephone()).thenReturn("service-telephone");
		when(service.getFax()).thenReturn("service-fax");
		when(service.getEmail()).thenReturn("service-email");
		when(service.getRootId()).thenReturn("group-id");
		when(service.getLayers()).thenReturn(emptyList());
		
		return service;
	}

	@Test
	public void testWrite() throws Exception {
		Service service = createEmptyService();
		
		String json = JsonService.toJson(service);
		assertNotNull(json);
		assertTrue(json.contains("formatRevision"));		
	}
	
	
	@Test
	public void testRead() throws Exception {
		String json = "{\"formatRevision\":\"1\","
				+ "\"id\":\"service-id\","
				+ "\"name\":\"service-name\","
				+ "\"title\":\"service-title\","
				+ "\"contact\":\"service-contact\","
				+ "\"organization\":\"service-organization\","
				+ "\"position\":\"service-position\","
				+ "\"addressType\":\"service-address-type\","
				+ "\"address\":\"service-address\","
				+ "\"city\":\"service-city\","
				+ "\"state\":\"service-state\","
				+ "\"zipcode\":\"service-zipcode\","
				+ "\"country\":\"service-country\","
				+ "\"telephone\":\"service-telephone\","
				+ "\"fax\":\"service-fax\","
				+ "\"email\":\"service-email\","
				+ "\"keywords\":[\"keyword0\",\"keyword1\"],"
				+ "\"abstract\":\"service-abstract\","
				+ "\"rootId\":\"group-id\","
				+ "\"layers\":[]}";
		
		Service service = JsonService.fromJson(json, Collections.emptyMap());
		assertEquals("service-id", service.getId());
		assertEquals("service-name", service.getName());
		assertEquals("service-title", service.getTitle());
		assertEquals("service-contact", service.getContact());
		assertEquals("service-organization", service.getOrganization());
		assertEquals("service-position", service.getPosition());
		assertEquals("service-address-type", service.getAddressType());
		assertEquals("service-address", service.getAddress());
		assertEquals("service-city", service.getCity());
		assertEquals("service-state", service.getState());
		assertEquals("service-zipcode", service.getZipcode());
		assertEquals("service-country", service.getCountry());
		assertEquals("service-telephone", service.getTelephone());
		assertEquals("service-fax", service.getFax());
		assertEquals("service-email", service.getEmail());
		assertEquals(Arrays.asList("keyword0", "keyword1"), service.getKeywords());
		assertEquals("service-abstract", service.getAbstract());
		assertEquals("group-id", service.getRootId());
		assertEquals(Collections.emptyList(), service.getLayers());	
	}
	
	@Test
	public void testWriteRead() {
		Service service = createEmptyService();
		
		String json = JsonService.toJson(service);
		JsonService.fromJson(json, Collections.emptyMap());
	}
	
	@Test
	public void testLayers() {
		VectorDatasetLayer firstDatasetLayerMock = mock(VectorDatasetLayer.class);
		when(firstDatasetLayerMock.getName()).thenReturn("layer-name-0");
		when(firstDatasetLayerMock.getTiling()).thenReturn(Optional.empty());
		when(firstDatasetLayerMock.getId()).thenReturn("layer-id-0");
		when(firstDatasetLayerMock.isVectorLayer()).thenReturn(true);
		when(firstDatasetLayerMock.asVectorLayer()).thenReturn(firstDatasetLayerMock);
		when(firstDatasetLayerMock.getTableName()).thenReturn("tableName");
		when(firstDatasetLayerMock.getImportTime()).thenReturn(Optional.of(new Timestamp(1)));
		
		DatasetLayerRef firstDatasetLayerRefMock = mock(DatasetLayerRef.class);
		when(firstDatasetLayerRefMock.isGroupRef()).thenReturn(false);
		when(firstDatasetLayerRefMock.asGroupRef()).thenThrow(new IllegalStateException());
		when(firstDatasetLayerRefMock.asDatasetRef()).thenReturn(firstDatasetLayerRefMock);
		when(firstDatasetLayerRefMock.getLayer()).thenReturn(firstDatasetLayerMock);
		
		Tiling tilingMock = mock(Tiling.class);
		when(tilingMock.getMimeFormats()).thenReturn(asList("image/jpg", "image/png"));
		
		VectorDatasetLayer secondDatasetLayerMock = mock(VectorDatasetLayer.class);
		when(secondDatasetLayerMock.getName()).thenReturn("layer-name-1");
		when(secondDatasetLayerMock.getTiling()).thenReturn(Optional.of(tilingMock));
		when(secondDatasetLayerMock.getId()).thenReturn("layer-id-1");
		when(secondDatasetLayerMock.isVectorLayer()).thenReturn(true);
		when(secondDatasetLayerMock.asVectorLayer()).thenReturn(secondDatasetLayerMock);
		when(secondDatasetLayerMock.getImportTime()).thenReturn(Optional.of(new Timestamp(2)));
		
		DatasetLayerRef secondDatasetLayerRefMock = mock(DatasetLayerRef.class);
		when(secondDatasetLayerRefMock.isGroupRef()).thenReturn(false);
		when(secondDatasetLayerRefMock.asGroupRef()).thenThrow(new IllegalStateException());
		when(secondDatasetLayerRefMock.asDatasetRef()).thenReturn(secondDatasetLayerRefMock);
		when(secondDatasetLayerRefMock.getLayer()).thenReturn(secondDatasetLayerMock);
		
		GroupLayer groupLayerMock = mock(GroupLayer.class);
		when(groupLayerMock.getTiling()).thenReturn(Optional.of(tilingMock));
		when(groupLayerMock.getId()).thenReturn("dataset-id-2");
		when(groupLayerMock.getLayers()).thenReturn(asList(firstDatasetLayerRefMock));
		
		GroupLayerRef groupLayerRefMock = mock(GroupLayerRef.class);
		when(groupLayerRefMock.isGroupRef()).thenReturn(true);
		when(groupLayerRefMock.asGroupRef()).thenReturn(groupLayerRefMock);
		when(groupLayerRefMock.asDatasetRef()).thenThrow(new IllegalStateException());
		when(groupLayerRefMock.getLayer()).thenReturn(groupLayerMock);
		
		Service serviceMock = mock(Service.class);
		when(serviceMock.getLayers()).thenReturn(asList(
				firstDatasetLayerRefMock, secondDatasetLayerRefMock, groupLayerRefMock));
		
		String json = JsonService.toJson(serviceMock);
		assertNotNull(json);
		
		Map<String, Optional<String>> metadataFileIdentifications = new HashMap<>();
		metadataFileIdentifications.put("layer-name-0", Optional.of ("metadata-file-id-0"));
		metadataFileIdentifications.put("layer-name-1", Optional.of ("metadata-file-id-1"));
		
		Service fromJson = JsonService.fromJson(json, metadataFileIdentifications);
		assertNotNull(fromJson);
		
		List<LayerRef<? extends Layer>> layerRefs = fromJson.getLayers();
		assertNotNull(layerRefs);
		
		Iterator<LayerRef<? extends Layer>> layerRefsItr = layerRefs.iterator();
		assertTrue(layerRefsItr.hasNext());
		
		LayerRef<?> layerRef = layerRefsItr.next();
		assertNotNull(layerRef);
		assertFalse(layerRef.isGroupRef());
		
		Layer layer = layerRef.getLayer();
		assertNotNull(layer);
		assertEquals("layer-id-0", layer.getId());
		assertFalse(layer.getTiling().isPresent());
		
		DatasetLayerRef datasetLayerRef = layerRef.asDatasetRef();
		assertNotNull(datasetLayerRef);
		
		DatasetLayer datasetLayer = datasetLayerRef.getLayer();
		assertNotNull(datasetLayer);
		assertEquals("metadata-file-id-0", datasetLayer.getMetadataFileIdentification().get());
		assertEquals("layer-id-0", datasetLayer.getId());
		
		assertTrue(datasetLayer.isVectorLayer());
		
		VectorDatasetLayer vectorDatasetLayer = datasetLayer.asVectorLayer();
		assertEquals("tableName", vectorDatasetLayer.getTableName());
		assertEquals(Optional.of(new Timestamp(1)), vectorDatasetLayer.getImportTime());
		
		assertTrue(layerRefsItr.hasNext());
		
		datasetLayer = layerRefsItr.next().asDatasetRef().getLayer();
		assertEquals(Optional.of(new Timestamp(2)), datasetLayer.getImportTime());
		
		Optional<Tiling> optionalTiling = datasetLayer.getTiling();
		assertTrue(optionalTiling.isPresent());
		
		Tiling tiling = optionalTiling.get();
		assertEquals(asList("image/jpg", "image/png"), tiling.getMimeFormats());
		
		assertTrue(layerRefsItr.hasNext());
		
		LayerRef<? extends Layer> groupLayerRef = layerRefsItr.next();
		assertNotNull(groupLayerRef);
		assertTrue(groupLayerRef.isGroupRef());
		
		GroupLayer groupLayer = groupLayerRef.asGroupRef().getLayer();
		assertNotNull(groupLayer);

		List<LayerRef<? extends Layer>> groupLayerContent = groupLayer.getLayers();
		assertEquals(1, groupLayerContent.size());
		
		assertFalse(layerRefsItr.hasNext());
	}
}
