package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.service.manager.QServiceStructure.serviceStructure;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QDatasetView.datasetView;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QImportJobColumn.importJobColumn;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QLeafLayerKeyword.leafLayerKeyword;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QServiceKeyword.serviceKeyword;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QJobState.jobState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.junit.Test;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.RasterDatasetLayer;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.domain.web.tree.Tiling;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.Clear;
import nl.idgis.publisher.recorder.messages.Cleared;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.recorder.messages.Wait;
import nl.idgis.publisher.recorder.messages.Waited;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.service.manager.messages.GetDatasetLayerRef;
import nl.idgis.publisher.service.manager.messages.GetGroupLayer;
import nl.idgis.publisher.service.manager.messages.GetPublishedService;
import nl.idgis.publisher.service.manager.messages.GetPublishedServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.GetServiceIndex;
import nl.idgis.publisher.service.manager.messages.GetServicesWithDataset;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.service.manager.messages.GetServicesWithStyle;
import nl.idgis.publisher.service.manager.messages.GetStyles;
import nl.idgis.publisher.service.manager.messages.PublishService;
import nl.idgis.publisher.service.manager.messages.PublishedServiceIndex;
import nl.idgis.publisher.service.manager.messages.ServiceIndex;
import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.service.style.TestStyle;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.AskResponse;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedIterable;

public class ServiceManagerTest extends AbstractServiceTest {
	
	public static class StreamRecorder extends AnyRecorder {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		public static Props props() {
			return Props.create(StreamRecorder.class);
		}
		
		@Override
		public void onRecord(Object msg, ActorRef sender) {
			log.debug("onRecord: {} {}", msg, sender);
			
			if(msg instanceof Item) {
				sender.tell(new NextItem(), getSelf());
			}
		}
	}
	
	int vectorDatasetId, rasterDatasetId;
		
	List<String> environmentIds;
	
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
		
		int vectorSourceDatasetId = insert(sourceDataset)
			.set(sourceDataset.identification, UUID.randomUUID().toString())
			.set(sourceDataset.metadataIdentification, UUID.randomUUID().toString())
			.set(sourceDataset.metadataFileIdentification, UUID.randomUUID().toString())
			.set(sourceDataset.externalIdentification, "sourcedataset0")
			.set(sourceDataset.dataSourceId, dataSourceId)
			.executeWithKey(sourceDataset.id);
		
		int vectorSourceDatasetVersionId = insert(sourceDatasetVersion)
			.set(sourceDatasetVersion.sourceDatasetId, vectorSourceDatasetId)
			.set(sourceDatasetVersion.categoryId, categoryId)
			.set(sourceDatasetVersion.name, "sourcedataset-name0")
			.set(sourceDatasetVersion.type, "VECTOR")
			.set(sourceDatasetVersion.confidential, false)
			.set(sourceDatasetVersion.metadataConfidential, false)
			.executeWithKey(sourceDatasetVersion.id);
		
		for(int i = 0; i < 2; i++) {
			insert(sourceDatasetVersionColumn)
			.set(sourceDatasetVersionColumn.sourceDatasetVersionId, vectorSourceDatasetVersionId)
			.set(sourceDatasetVersionColumn.index, i)
			.set(sourceDatasetVersionColumn.name, "column" + i)
			.set(sourceDatasetVersionColumn.dataType, "TEXT")
			.execute();
		}
		
		vectorDatasetId = insert(dataset)
			.set(dataset.identification, "dataset0")
			.set(dataset.name, "dataset-name0")			
			.set(dataset.sourceDatasetId, vectorSourceDatasetId)
			.set(dataset.metadataIdentification, UUID.randomUUID().toString())
			.set(dataset.metadataFileIdentification, "dataset-metadata-0")
			.executeWithKey(dataset.id);
		
		insert(datasetColumn)
			.columns(
				datasetColumn.datasetId,
				datasetColumn.index,
				datasetColumn.name,
				datasetColumn.dataType)
			.select(new SQLSubQuery().from(sourceDatasetVersionColumn)
				.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(vectorSourceDatasetVersionId))
				.list(
					vectorDatasetId,
					sourceDatasetVersionColumn.index,
					sourceDatasetVersionColumn.name,
					sourceDatasetVersionColumn.dataType))
			.execute();
		
		try(Statement stmt = statement();) {
			stmt.execute("create schema staging_data");
		}
		
		query().from(dataset)
		.list(dataset.identification).forEach(datasetId -> {			
			try(Statement stmt = statement();) {
				stmt.execute("create table staging_data." + datasetId + "(" +
					query().from(datasetColumn)
						.join(dataset).on(dataset.id.eq(datasetColumn.datasetId))
						.where(dataset.identification.eq(datasetId))
						.orderBy(datasetColumn.index.asc())
						.list(datasetColumn.name.concat(" ").concat(datasetColumn.dataType)).stream()
							.collect(Collectors.joining(",")) + ")");
			} catch(Exception e) { 
				throw new RuntimeException(e);
			}
		});
		
		int vectorJobId = insert(job)
			.set(job.type, "IMPORT")
			.executeWithKey(job.id);
		
		int vectorImportJobId = insert(importJob)
			.set(importJob.jobId, vectorJobId)
			.set(importJob.sourceDatasetVersionId, vectorSourceDatasetVersionId)
			.set(importJob.datasetId, vectorDatasetId)
			.set(importJob.filterConditions, "{ \"expression\": null }")
			.executeWithKey(importJob.id);
		
		insert(importJobColumn)
			.columns(
				importJobColumn.importJobId,
				importJobColumn.index,
				importJobColumn.name,
				importJobColumn.dataType)
			.select(new SQLSubQuery().from(datasetColumn)
				.where(datasetColumn.datasetId.eq(vectorDatasetId))
				.list(
					vectorImportJobId,
					datasetColumn.index,
					datasetColumn.name,
					datasetColumn.dataType))
			.execute();
		
		insert(jobState)
			.columns(
				jobState.jobId,
				jobState.state)
			.values(vectorJobId, "STARTED").addBatch()
			.values(vectorJobId, "SUCCEEDED").addBatch()
			.execute();
		
		int rasterSourceDatasetId = insert(sourceDataset)
			.set(sourceDataset.identification, UUID.randomUUID().toString())
			.set(sourceDataset.metadataIdentification, UUID.randomUUID().toString())
			.set(sourceDataset.metadataFileIdentification, UUID.randomUUID().toString())
			.set(sourceDataset.externalIdentification, "sourcedataset1")
			.set(sourceDataset.dataSourceId, dataSourceId)
			.executeWithKey(sourceDataset.id);
		
		int rasterSourceDatasetVersionId = insert(sourceDatasetVersion)
			.set(sourceDatasetVersion.sourceDatasetId, rasterSourceDatasetId)
			.set(sourceDatasetVersion.categoryId, categoryId)
			.set(sourceDatasetVersion.name, "sourcedataset-name1")
			.set(sourceDatasetVersion.type, "RASTER")
			.set(sourceDatasetVersion.confidential, false)
			.set(sourceDatasetVersion.metadataConfidential, false)
			.executeWithKey(sourceDatasetVersion.id);
		
		rasterDatasetId = insert(dataset)
			.set(dataset.identification, "dataset1")
			.set(dataset.name, "dataset-name1")			
			.set(dataset.sourceDatasetId, rasterSourceDatasetId)
			.set(dataset.metadataIdentification, UUID.randomUUID().toString())
			.set(dataset.metadataFileIdentification, UUID.randomUUID().toString())
			.executeWithKey(dataset.id);
		
		int rasterJobId = insert(job)
			.set(job.type, "IMPORT")
			.executeWithKey(job.id);
		
		insert(importJob)
			.set(importJob.jobId, rasterJobId)
			.set(importJob.sourceDatasetVersionId, rasterSourceDatasetVersionId)
			.set(importJob.datasetId, rasterDatasetId)
			.set(importJob.filterConditions, "{ \"expression\": null }")
			.executeWithKey(importJob.id);
		
		insert(jobState)
			.columns(
				jobState.jobId,
				jobState.state)
			.values(rasterJobId, "STARTED").addBatch()
			.values(rasterJobId, "SUCCEEDED").addBatch()
			.execute();	
		
		environmentIds = new ArrayList<>();
		environmentIds.add("environment0");
		environmentIds.add("environment1");
		environmentIds.add("environment2");
		
		for(String environmentId : environmentIds) {
			insert(environment)
				.set(environment.identification, environmentId)
				.set(environment.name, environmentId + "-name")
				.set(environment.url, "http://" + environmentId + ".example/")
				.execute();
		}
	}
	
	@Test
	public void testVectorRasterMix() throws Exception {
		int vectorLayerId = insert(genericLayer)
			.set(genericLayer.identification, "vector")
			.set(genericLayer.name, "vector-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, vectorLayerId)
			.set(leafLayer.datasetId, vectorDatasetId)
			.executeWithKey(leafLayer.id);
		
		int rasterLayerId = insert(genericLayer)
			.set(genericLayer.identification, "raster")
			.set(genericLayer.name, "raster-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, rasterLayerId)
			.set(leafLayer.datasetId, rasterDatasetId)
			.executeWithKey(leafLayer.id);
		
		int serviceLayerId = insert(genericLayer)
			.set(genericLayer.identification, "service")
			.set(genericLayer.name, "service-name")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.columns(
				layerStructure.childLayerId,
				layerStructure.parentLayerId,
				layerStructure.layerOrder)
			.values(vectorLayerId, serviceLayerId, 0).addBatch()
			.values(rasterLayerId, serviceLayerId, 1).addBatch()
			.execute();
		
		insert(service)
			.set(service.genericLayerId, serviceLayerId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.executeWithKey(service.id);
		
		Service service = f.ask(serviceManager, new GetService("service"), Service.class).get();
		
		List<LayerRef<? extends Layer>> layerRefs = service.getLayers();
		assertNotNull(layerRefs);
		
		Iterator<LayerRef<? extends Layer>> layerRefItr = layerRefs.iterator();
		assertNotNull(layerRefItr);
		
		assertTrue(layerRefItr.hasNext());
		LayerRef<? extends Layer> vectorLayerRef = layerRefItr.next();
		assertNotNull(vectorLayerRef);
		assertFalse(vectorLayerRef.isGroupRef());
		
		DatasetLayer vectorLayer = vectorLayerRef.asDatasetRef().getLayer();
		assertNotNull(vectorLayer);
		assertTrue(vectorLayer.isVectorLayer());
		
		VectorDatasetLayer vectorDatasetLayer = vectorLayer.asVectorLayer();
		assertNotNull(vectorDatasetLayer);
		assertEquals("dataset0", vectorDatasetLayer.getTableName());
		assertEquals(Arrays.asList("column0", "column1"), vectorDatasetLayer.getColumnNames());
		
		assertTrue(layerRefItr.hasNext());
		LayerRef<? extends Layer> rasterLayerRef = layerRefItr.next();
		assertNotNull(rasterLayerRef);
		assertFalse(rasterLayerRef.isGroupRef());
		
		DatasetLayer rasterLayer = rasterLayerRef.asDatasetRef().getLayer();
		assertNotNull(rasterLayer);
		assertTrue(rasterLayer.isRasterLayer());
		
		RasterDatasetLayer rasterDatasetLayer = rasterLayer.asRasterLayer();
		assertNotNull(rasterDatasetLayer);
		assertEquals("dataset1.tif", rasterDatasetLayer.getFileName());
		
		assertFalse(layerRefItr.hasNext());
	}
		
	@Test
	public void testSingleLayer() throws Exception {
		int layerId0 = insert(genericLayer)
			.set(genericLayer.identification, "layer0")
			.set(genericLayer.name, "layer-name0")
			.executeWithKey(genericLayer.id);
		
		int tiledLayerId0 = insert(tiledLayer)
			.set(tiledLayer.genericLayerId, layerId0)
			.set(tiledLayer.metaWidth, 4)
			.set(tiledLayer.metaHeight, 6)
			.set(tiledLayer.expireCache, 1)
			.set(tiledLayer.expireClients, 2)
			.set(tiledLayer.gutter, 5)
			.executeWithKey(tiledLayer.id);
		
		insert(tiledLayerMimeformat)
			.set(tiledLayerMimeformat.tiledLayerId, tiledLayerId0)
			.set(tiledLayerMimeformat.mimeformat, "image/png")
			.execute();
		
		insert(tiledLayerMimeformat)
			.set(tiledLayerMimeformat.tiledLayerId, tiledLayerId0)
			.set(tiledLayerMimeformat.mimeformat, "image/jpg")
			.execute();
		
		int leafLayerId = insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId0)
			.set(leafLayer.datasetId, vectorDatasetId)
			.executeWithKey(leafLayer.id);
		
		insert(leafLayerKeyword)
			.set(leafLayerKeyword.leafLayerId, leafLayerId)
			.set(leafLayerKeyword.keyword, "keyword0")
			.execute();
		
		insert(leafLayerKeyword)
			.set(leafLayerKeyword.leafLayerId, leafLayerId)
			.set(leafLayerKeyword.keyword, "keyword1")
			.execute();
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		
		StringWriter sw = new StringWriter();		
		t.transform(new DOMSource(TestStyle.getGreenSld()), new StreamResult(sw));
		
		String styleDefinition = sw.getBuffer().toString();
		
		int styleId0 = insert(style)
			.set(style.identification, "style0")
			.set(style.name, "styleName0")			
			.set(style.definition, styleDefinition)
			.set(style.styleType, "POINT")
			.executeWithKey(style.id);
		
		insert(layerStyle)
			.set(layerStyle.layerId, leafLayerId)
			.set(layerStyle.styleId, styleId0)
			.execute();
		
		int styleId1 = insert(style)
			.set(style.identification, "style1")
			.set(style.name, "styleName1")
			.set(style.definition, styleDefinition)
			.set(style.styleType, "POINT")
			.executeWithKey(style.id);
		
		insert(layerStyle)
			.set(layerStyle.layerId, leafLayerId)
			.set(layerStyle.styleId, styleId1)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.set(genericLayer.title, "serviceTitle0")
			.set(genericLayer.abstractCol, "serviceAbstract0")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, layerId0)
			.set(layerStructure.styleId, styleId0)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, layerId0)
			.set(layerStructure.styleId, styleId1)
			.set(layerStructure.layerOrder, 1)
			.execute();
		
		delete(constants).execute();
		
		int constantsId = insert(constants)
			.set(constants.contact, "serviceContact0")
			.set(constants.organization, "serviceOrganization0")
			.set(constants.position, "servicePosition0")
			.set(constants.addressType, "serviceAdressType0")
			.set(constants.address, "serviceAdress0")
			.set(constants.city, "serviceCity0")
			.set(constants.state, "serviceState0")
			.set(constants.zipcode, "serviceZipcode0")
			.set(constants.country, "serviceCountry0")
			.set(constants.telephone, "serviceTelephone0")
			.set(constants.fax, "serviceFax0")
			.set(constants.email, "serviceEmail0")
			.executeWithKey(constants.id);
		
		int serviceId = insert(service)
			.set(service.genericLayerId, rootId)
			.set(service.constantsId, constantsId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.executeWithKey(service.id);
		
		insert(serviceKeyword)
			.set(serviceKeyword.serviceId, serviceId)
			.set(serviceKeyword.keyword, "keyword2")
			.execute();
		
		insert(serviceKeyword)
			.set(serviceKeyword.serviceId, serviceId)
			.set(serviceKeyword.keyword, "keyword3")
			.execute();
		
		Service service = f.ask(serviceManager, new GetService("rootgroup"), Service.class).get();		
		assertEquals("rootgroup", service.getRootId());
		assertEquals("rootgroup-name", service.getName());
		assertEquals("serviceTitle0", service.getTitle());
		assertEquals("serviceAbstract0", service.getAbstract());
		assertEquals("serviceContact0", service.getContact());
		
		List<String> serviceKeywords = service.getKeywords();
		assertEquals(2, serviceKeywords.size());
		assertTrue(serviceKeywords.contains("keyword2"));
		assertTrue(serviceKeywords.contains("keyword3"));
		
		List<LayerRef<?>> layers = service.getLayers();
		assertNotNull(layers);
		
		Iterator<LayerRef<?>> itr = layers.iterator();
		assertTrue(itr.hasNext());
		
		LayerRef<?> layerRef = itr.next();
		assertNotNull(layerRef);
		assertFalse(layerRef.isGroupRef());
		
		DatasetLayerRef datasetLayerRef = layerRef.asDatasetRef();
		Optional<StyleRef> optionalStyleRef = datasetLayerRef.getStyleRef();
		assertNotNull(optionalStyleRef);
		assertTrue(optionalStyleRef.isPresent());
		
		StyleRef styleRef = optionalStyleRef.get();
		assertEquals("style0", styleRef.getId());
		assertEquals("styleName0", styleRef.getName());
		
		DatasetLayer datasetLayer = datasetLayerRef.getLayer();
		assertEquals("dataset-metadata-0", datasetLayer.getMetadataFileIdentification().get());
		assertEquals("layer0", datasetLayer.getId());
		
		assertTrue(datasetLayer.isVectorLayer());
		VectorDatasetLayer vectorDatasetLayer = datasetLayer.asVectorLayer();
		assertEquals("dataset0", vectorDatasetLayer.getTableName());
		
		List<String> keywords = datasetLayer.getKeywords();
		assertEquals(2, keywords.size());
		assertTrue(keywords.contains("keyword0"));
		assertTrue(keywords.contains("keyword1"));
		
		List<String> columnNames = vectorDatasetLayer.getColumnNames();
		assertEquals(Arrays.asList("column0", "column1"), columnNames);
		
		Optional<Tiling> optionalTiling = datasetLayer.getTiling();
		assertTrue(optionalTiling.isPresent());		
		
		Tiling tiling = optionalTiling.get();
		assertEquals(Integer.valueOf(4), tiling.getMetaWidth());
		assertEquals(Integer.valueOf(6), tiling.getMetaHeight());
		assertEquals(Integer.valueOf(1), tiling.getExpireCache());
		assertEquals(Integer.valueOf(2), tiling.getExpireClients());
		assertEquals(Integer.valueOf(5), tiling.getGutter());
		
		List<String> mimeFormats = tiling.getMimeFormats();		
		assertTrue(mimeFormats.contains("image/png"));
		assertTrue(mimeFormats.contains("image/jpg"));
		assertEquals(2, mimeFormats.size());
		
		List<StyleRef> styleRefs = datasetLayer.getStyleRefs();
		assertNotNull(styleRefs);
		
		Iterator<StyleRef> styleRefsItr = styleRefs.iterator();
		assertTrue(styleRefsItr.hasNext());
		
		styleRef = styleRefsItr.next();
		assertNotNull(styleRef);
		assertEquals("styleName0", styleRef.getName());
		
		assertTrue(styleRefsItr.hasNext());
		
		styleRef = styleRefsItr.next();
		assertNotNull(styleRef);
		assertEquals("styleName1", styleRef.getName());
				
		assertTrue(itr.hasNext());
		
		layerRef = itr.next();
		assertFalse(layerRef.isGroupRef());
		datasetLayerRef = layerRef.asDatasetRef();
		optionalStyleRef = datasetLayerRef.getStyleRef();
		assertNotNull(optionalStyleRef);
		assertTrue(optionalStyleRef.isPresent());
		
		styleRef = optionalStyleRef.get();
		assertEquals("styleName1", styleRef.getName());
		
		assertFalse(itr.hasNext());
		
		ActorRef recorder = actorOf(StreamRecorder.props(), "stream-recorder");
		serviceManager.tell(new GetStyles("rootgroup"), recorder);
		
		f.ask(recorder, new Wait(3), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(Item.class, item -> {
				assertTrue(item.getContent() instanceof Style);
			})
			.assertNext(Item.class, item -> {
				assertTrue(item.getContent() instanceof Style);
			})
			.assertNext(End.class)
			.assertNotHasNext();
		
		f.ask(recorder, new Clear(), Cleared.class).get();		
		serviceManager.tell(new GetStyles("nonExistingService"), recorder);
		
		f.ask(recorder, new Wait(1), Waited.class).get();
		f.ask(recorder, new GetRecording(), Recording.class).get()
			.assertNext(End.class)
			.assertNotHasNext();
		
		TypedIterable<?> services = f.ask(serviceManager, new GetServicesWithStyle("style0"), TypedIterable.class).get();
		assertTrue(services.contains(String.class));
		
		Iterator<String> servicesItr = services.cast(String.class).iterator();
		assertTrue(servicesItr.hasNext());
		assertEquals("rootgroup", servicesItr.next());
		assertFalse(servicesItr.hasNext());
		
		services = f.ask(serviceManager, new GetServicesWithDataset("dataset0"), TypedIterable.class).get();
		assertTrue(services.contains(String.class));
		
		servicesItr = services.cast(String.class).iterator();
		assertTrue(servicesItr.hasNext());
		assertEquals("rootgroup", servicesItr.next());
		assertFalse(servicesItr.hasNext());
	}
	
	@Test
	public void testGroup() throws Exception {
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		int tiledLayerId = insert(tiledLayer)
			.set(tiledLayer.genericLayerId, layerId)
			.set(tiledLayer.metaWidth, 4)
			.set(tiledLayer.metaHeight, 6)
			.set(tiledLayer.expireCache, 1)
			.set(tiledLayer.expireClients, 2)
			.set(tiledLayer.gutter, 5)
			.executeWithKey(tiledLayer.id);
			
		insert(tiledLayerMimeformat)
			.set(tiledLayerMimeformat.tiledLayerId, tiledLayerId)
			.set(tiledLayerMimeformat.mimeformat, "image/png")
			.execute();
		
		insert(tiledLayerMimeformat)
			.set(tiledLayerMimeformat.tiledLayerId, tiledLayerId)
			.set(tiledLayerMimeformat.mimeformat, "image/jpg")
			.execute();
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
			.set(leafLayer.datasetId, vectorDatasetId)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.executeWithKey(genericLayer.id);
		
		int groupId = insert(genericLayer)
			.set(genericLayer.identification, "group")
			.set(genericLayer.name, "group-name")
			.executeWithKey(genericLayer.id);
		
		int tiledGroupLayerId = insert(tiledLayer)
			.set(tiledLayer.genericLayerId, groupId)
			.set(tiledLayer.metaWidth, 4)
			.set(tiledLayer.metaHeight, 6)
			.set(tiledLayer.expireCache, 1)
			.set(tiledLayer.expireClients, 2)
			.set(tiledLayer.gutter, 5)
			.executeWithKey(tiledLayer.id);
		
		insert(tiledLayerMimeformat)
			.set(tiledLayerMimeformat.tiledLayerId, tiledGroupLayerId)
			.set(tiledLayerMimeformat.mimeformat, "image/png")
			.execute();
		
		insert(tiledLayerMimeformat)
			.set(tiledLayerMimeformat.tiledLayerId, tiledGroupLayerId)
			.set(tiledLayerMimeformat.mimeformat, "image/jpg")
			.execute();
		
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
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Service service = f.ask(serviceManager, new GetService("rootgroup"), Service.class).get();
		
		List<LayerRef<?>> rootLayers = service.getLayers();
		assertNotNull(rootLayers);
		
		Iterator<LayerRef<?>> rootItr = rootLayers.iterator();
		assertTrue(rootItr.hasNext());
		
		LayerRef<?> groupRef = rootItr.next();
		assertNotNull(groupRef);
		assertTrue(groupRef.isGroupRef());
		
		GroupLayer groupLayer = groupRef.asGroupRef().getLayer();
		assertEquals("group", groupLayer.getId());
		assertEquals("group-name", groupLayer.getName());
		
		Optional<Tiling> optionalTiling = groupLayer.getTiling();
		assertTrue(optionalTiling.isPresent());
		
		Tiling tiling = optionalTiling.get();
		assertEquals(Integer.valueOf(4), tiling.getMetaWidth());
		assertEquals(Integer.valueOf(6), tiling.getMetaHeight());
		assertEquals(Integer.valueOf(1), tiling.getExpireCache());
		assertEquals(Integer.valueOf(2), tiling.getExpireClients());
		assertEquals(Integer.valueOf(5), tiling.getGutter());
		
		List<String> mimeFormats = tiling.getMimeFormats();		
		assertTrue(mimeFormats.contains("image/png"));
		assertTrue(mimeFormats.contains("image/jpg"));
		assertEquals(2, mimeFormats.size());
		
		List<LayerRef<?>> groupLayers = groupLayer.getLayers();
		assertNotNull(groupLayers);
		
		Iterator<LayerRef<?>> groupItr = groupLayers.iterator();
		assertTrue(groupItr.hasNext());
		
		LayerRef<?> layerRef = groupItr.next();
		assertNotNull(layerRef);
		assertFalse(layerRef.isGroupRef());
		
		DatasetLayer layer = layerRef.asDatasetRef().getLayer();
		assertNotNull(layer);
		
		optionalTiling = layer.getTiling();
		assertTrue(optionalTiling.isPresent());
		
		tiling = optionalTiling.get();
		assertEquals(Integer.valueOf(4), tiling.getMetaWidth());
		assertEquals(Integer.valueOf(6), tiling.getMetaHeight());
		assertEquals(Integer.valueOf(1), tiling.getExpireCache());
		assertEquals(Integer.valueOf(2), tiling.getExpireClients());
		assertEquals(Integer.valueOf(5), tiling.getGutter());
		
		mimeFormats = tiling.getMimeFormats();		
		assertTrue(mimeFormats.contains("image/png"));
		assertTrue(mimeFormats.contains("image/jpg"));
		assertEquals(2, mimeFormats.size());
		
		assertFalse(groupItr.hasNext());
		
		assertFalse(rootItr.hasNext());
		
		groupLayer = f.ask(serviceManager, new GetGroupLayer("group"), GroupLayer.class).get();
		assertEquals("group", groupLayer.getId());
		assertEquals("group-name", groupLayer.getName());
		
		optionalTiling = groupLayer.getTiling();
		assertTrue(optionalTiling.isPresent());
		
		tiling = optionalTiling.get();
		assertEquals(Integer.valueOf(4), tiling.getMetaWidth());
		assertEquals(Integer.valueOf(6), tiling.getMetaHeight());
		assertEquals(Integer.valueOf(1), tiling.getExpireCache());
		assertEquals(Integer.valueOf(2), tiling.getExpireClients());
		assertEquals(Integer.valueOf(5), tiling.getGutter());
		
		mimeFormats = tiling.getMimeFormats();		
		assertTrue(mimeFormats.contains("image/png"));
		assertTrue(mimeFormats.contains("image/jpg"));
		assertEquals(2, mimeFormats.size());
		
		groupLayers = groupLayer.getLayers();
		assertNotNull(groupLayers);
		
		groupItr = groupLayers.iterator();
		assertTrue(groupItr.hasNext());
		
		layerRef = groupItr.next();		
		assertNotNull(layerRef);
		assertFalse(layerRef.isGroupRef());
		
		layer = layerRef.asDatasetRef().getLayer();
		assertNotNull(layer);
		
		optionalTiling = layer.getTiling();
		assertTrue(optionalTiling.isPresent());
		
		tiling = optionalTiling.get();
		assertEquals(Integer.valueOf(4), tiling.getMetaWidth());
		assertEquals(Integer.valueOf(6), tiling.getMetaHeight());
		assertEquals(Integer.valueOf(1), tiling.getExpireCache());
		assertEquals(Integer.valueOf(2), tiling.getExpireClients());
		assertEquals(Integer.valueOf(5), tiling.getGutter());
		
		mimeFormats = tiling.getMimeFormats();		
		assertTrue(mimeFormats.contains("image/png"));
		assertTrue(mimeFormats.contains("image/jpg"));
		assertEquals(2, mimeFormats.size());
		
		assertFalse(groupItr.hasNext());
		
		assertFalse(rootItr.hasNext());
		
		TypedIterable<?> services = f.ask(serviceManager, new GetServicesWithLayer("rootgroup"), TypedIterable.class).get();
		assertTrue(services.contains(String.class));
		
		Iterator<String> servicesItr = services.cast(String.class).iterator();
		assertFalse(servicesItr.hasNext());
		
		services = f.ask(serviceManager, new GetServicesWithLayer("group"), TypedIterable.class).get();
		assertTrue(services.contains(String.class));
		
		servicesItr = services.cast(String.class).iterator();
		assertTrue(servicesItr.hasNext());
		assertEquals("rootgroup", servicesItr.next());
		assertFalse(servicesItr.hasNext());
		
		services = f.ask(serviceManager, new GetServicesWithDataset("dataset0"), TypedIterable.class).get();
		assertTrue(services.contains(String.class));
		
		servicesItr = services.cast(String.class).iterator();
		assertTrue(servicesItr.hasNext());
		assertEquals("rootgroup", servicesItr.next());
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
			.set(leafLayer.datasetId, vectorDatasetId)
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
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Service service = f.ask(serviceManager, new GetService("rootgroup"), Service.class).get();
		
		List<LayerRef<?>> rootLayers = service.getLayers();
		assertNotNull(rootLayers);
		
		Iterator<LayerRef<?>> rootItr = rootLayers.iterator();
		assertTrue(rootItr.hasNext());
		
		LayerRef<?> group0Ref = rootItr.next();		
		assertNotNull(group0Ref);
		assertTrue(group0Ref.isGroupRef());
		
		GroupLayer group0 = group0Ref.asGroupRef().getLayer();
		assertNotNull(group0);		
		assertEquals("group0", group0.getId());
		
		List<LayerRef<?>> group0Layers = group0.getLayers();
		assertNotNull(group0Layers);
		
		Iterator<LayerRef<?>> group0Itr = group0Layers.iterator();
		assertTrue(group0Itr.hasNext());
		
		LayerRef<?> group1Ref = group0Itr.next();
		assertTrue(group1Ref.isGroupRef());
		assertNotNull(group1Ref);
		
		GroupLayer group1 = group1Ref.asGroupRef().getLayer();
		assertNotNull(group1);
		
		assertEquals("group1", group1.getId());
		
		List<LayerRef<?>> group1Layers = group1.getLayers();		
		assertNotNull(group1Layers);
		
		Iterator<LayerRef<?>> group1Itr = group1Layers.iterator();
		assertTrue(group1Itr.hasNext());
		
		LayerRef<?> layerRef = group1Itr.next();
		assertFalse(layerRef.isGroupRef());
		assertNotNull(layerRef);
		
		Layer layer = layerRef.getLayer();
		assertNotNull(layer);
		
		assertFalse(group1Itr.hasNext());
		
		assertFalse(group0Itr.hasNext());
		
		assertFalse(rootItr.hasNext());
		
		GroupLayer groupLayer = f.ask(serviceManager, new GetGroupLayer("group0"), GroupLayer.class).get();
		assertEquals("group0", groupLayer.getId());
		
		group0Layers = groupLayer.getLayers();
		assertNotNull(group0Layers);
		
		group0Itr = group0Layers.iterator();
		assertTrue(group0Itr.hasNext());
		
		group1Ref = group0Itr.next();
		assertNotNull(group1Ref);
		assertTrue(group1Ref.isGroupRef());
		
		group1 = group1Ref.asGroupRef().getLayer();
		assertNotNull(group1);		
		assertEquals("group1", group1.getId());
		
		group1Layers = group1.getLayers();		
		assertNotNull(group1Layers);
		
		group1Itr = group1Layers.iterator();
		assertTrue(group1Itr.hasNext());
		
		layerRef = group1Itr.next();
		assertNotNull(layerRef);
		assertFalse(layerRef.isGroupRef());
		
		layer = layerRef.getLayer();
		assertNotNull(layer);
		
		assertFalse(group1Itr.hasNext());
		
		assertFalse(group0Itr.hasNext());
		
		assertFalse(rootItr.hasNext());
	}
	
	@Test
	public void testNotFound() throws Exception {
		f.ask(serviceManager, new GetService("service0"), NotFound.class).get();
	}
	
	@Test
	public void testMultipleServices() throws Exception {
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
			.set(leafLayer.datasetId, vectorDatasetId)
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
			.set(service.genericLayerId, root0Id)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		insert(service)
			.set(service.genericLayerId, root1Id)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Service service0 = f.ask(serviceManager, new GetService("rootgroup0"), Service.class).get();
		
		List<LayerRef<?>> service0Layers = service0.getLayers();
		assertNotNull(service0Layers);
		
		Iterator<LayerRef<?>> service0Itr = service0Layers.iterator();
		assertTrue(service0Itr.hasNext());
		
		LayerRef<?> service0LayerRef = service0Itr.next();
		assertNotNull(service0LayerRef);
		assertFalse(service0LayerRef.isGroupRef());
		
		DatasetLayer service0Layer = service0LayerRef.asDatasetRef().getLayer();
		assertNotNull(service0Layer);
		
		assertEquals("layer", service0Layer.getId());
		
		assertFalse(service0Itr.hasNext());
		
		Service service1 = f.ask(serviceManager, new GetService("rootgroup1"), Service.class).get();
		
		List<LayerRef<?>> service1Layers = service1.getLayers();
		assertNotNull(service1Layers);
		
		Iterator<LayerRef<?>> service1Itr = service1Layers.iterator();
		assertTrue(service1Itr.hasNext());
		
		LayerRef<?> groupRef = service1Itr.next();
		assertNotNull(groupRef);
		assertTrue(groupRef.isGroupRef());
		
		GroupLayer group = groupRef.asGroupRef().getLayer();
		assertNotNull(group);
		
		List<LayerRef<?>> groupLayers = group.getLayers();
		assertNotNull(groupLayers);
		
		Iterator<LayerRef<?>> groupItr = groupLayers.iterator();
		assertTrue(groupItr.hasNext());
		
		LayerRef<?> service1LayerRef = groupItr.next();
		assertNotNull(service1LayerRef);
		assertFalse(service1LayerRef.isGroupRef());
		
		Layer service1Layer = service1LayerRef.getLayer();
		assertNotNull(service1Layer);
		
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
				.set(leafLayer.datasetId, vectorDatasetId)
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
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();

		Service service = f.ask(serviceManager, new GetService("rootgroup"), Service.class).get();
		assertEquals("rootgroup", service.getRootId());
		
		List<LayerRef<?>> layers = service.getLayers();		
		assertNotNull(layers);
		
		Iterator<LayerRef<?>> layersItr = layers.iterator();
		for(String layerIdentification : layerIdentifications) {
			assertTrue(layersItr.hasNext());
			
			LayerRef<?> layerRef = layersItr.next();
			assertNotNull(layerRef);
			
			Layer layer = layerRef.getLayer();
			assertNotNull(layer);
			assertEquals(layerIdentification, layer.getId());
		}
		
		assertFalse(layersItr.hasNext());
	}
	
	@Test
	public void testServiceIndex() throws Exception {
		ServiceIndex serviceIndex = f.ask(serviceManager, new GetServiceIndex(), ServiceIndex.class).get();
		
		List<String> serviceNames = serviceIndex.getServiceNames();
		assertNotNull(serviceNames);
		assertTrue(serviceNames.isEmpty());
		
		List<String> styleNames = serviceIndex.getStyleNames();
		assertNotNull(styleNames);
		assertTrue(styleNames.isEmpty());
		
		int layerId0 = insert(genericLayer)
			.set(genericLayer.identification, "layer0")
			.set(genericLayer.name, "layer-name0")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId0)
			.set(leafLayer.datasetId, vectorDatasetId)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "rootgroup")
			.set(genericLayer.name, "rootgroup-name")
			.set(genericLayer.title, "serviceTitle0")
			.set(genericLayer.abstractCol, "serviceAbstract0")
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, layerId0)
			.set(layerStructure.layerOrder, 0)
			.execute();
	
		insert(service)
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		serviceIndex = f.ask(serviceManager, new GetServiceIndex(), ServiceIndex.class).get();
		
		serviceNames = serviceIndex.getServiceNames();
		assertNotNull(serviceNames);
		
		Iterator<String> serviceNameItr = serviceNames.iterator();
		assertTrue(serviceNameItr.hasNext());		
		assertEquals("rootgroup-name", serviceNameItr.next());		
		assertFalse(serviceNameItr.hasNext());
		
		styleNames = serviceIndex.getStyleNames();
		assertNotNull(styleNames);
		assertTrue(styleNames.isEmpty());
	}
	
	@Test
	public void testComplexGroupStructure() throws Exception {
		insert(genericLayer)
			.columns(
				genericLayer.id,
				genericLayer.name,
				genericLayer.title,
				genericLayer.abstractCol,
				genericLayer.identification)
			.values(1, "Aantal_koop-_en_huurwoningen_per_buurt", "", "", "fd12e3ef-5ae6-4fed-acbc-0dfad83c8936").addBatch()
			.values(2, "koop-_en_huurwoningen_per_buurt", "titel", "", "81567e62-9e32-47ec-8c92-eae119e81957").addBatch()				
			.values(11, "ondergroep1", "", "", "6fc6a513-c625-4902-a30f-c637a5a311b8").addBatch()
			.values(12, "ondergroep2", "", "", "bb0eed56-85be-4817-8495-2810a53abf89").addBatch()
			.values(16, "bovenstegroep", "", "", "6a132ba4-ee31-49f3-9ed1-304a021f85a8").addBatch()
			.values(14, "bovengroep2", "", "", "4da7fd70-c2b2-4513-9917-e3ed417cce87").addBatch()
			.values(17, "supergroep", "", "", "c9d91350-73fc-4cf4-91e6-d3c5b51276f4").addBatch()				
			.execute();
		
		insert(layerStructure)
			.columns(
				layerStructure.parentLayerId,
				layerStructure.childLayerId,
				layerStructure.layerOrder,
				layerStructure.styleId) 
			.values(11, 2, 0, null).addBatch()
			.values(12, 1, 0, null).addBatch()
			.values(16, 14, 0, null).addBatch()
			.values(14, 11, 0, null).addBatch()
			.values(14, 12, 1, null).addBatch()
			.values(17, 16, 0, null).addBatch()		
			.execute();

		insert(leafLayer)
			.columns(
				leafLayer.id,
				leafLayer.metadata,
				leafLayer.filter,
				leafLayer.genericLayerId,
				leafLayer.datasetId)
			.values(1, null, null, 1, vectorDatasetId).addBatch()
			.values(2, null, null, 2, vectorDatasetId).addBatch()
			.execute();
		
		for(String identification : Arrays.asList(
				"6fc6a513-c625-4902-a30f-c637a5a311b8",
				"bb0eed56-85be-4817-8495-2810a53abf89",
				"6a132ba4-ee31-49f3-9ed1-304a021f85a8",
				"4da7fd70-c2b2-4513-9917-e3ed417cce87",
				"c9d91350-73fc-4cf4-91e6-d3c5b51276f4")) {
			
			GroupLayer groupLayer = f.ask(serviceManager, new GetGroupLayer(identification), GroupLayer.class).get();
			assertValidGroupLayer(identification, groupLayer);
		}
	}
	
	private void assertValidGroupLayer(String identification, GroupLayer groupLayer) {
		List<LayerRef<?>> layers = groupLayer.getLayers();
		assertNotNull(layers);
		
		for(LayerRef<?> layerRef : layers) {
			if(layerRef.isGroupRef()) {
				GroupLayer layer = layerRef.asGroupRef().getLayer();
				assertNotNull(layer);
				
				assertValidGroupLayer(identification, layer);
			} else {
				DatasetLayer layer = layerRef.asDatasetRef().getLayer();
				assertNotNull(layer);
			}
		}
	}
	
	@Test
	public void testCycle() throws Exception {
		insert(genericLayer)
			.columns(
				genericLayer.id,
				genericLayer.name,
				genericLayer.title,
				genericLayer.abstractCol,
				genericLayer.identification)
			.values(0, "service", "title0", "abstract0", "service").addBatch()
			.values(1, "group0", "title1", "abstract1", "group0").addBatch()
			.values(2, "group1", "title2", "abstract2", "group1").addBatch()
			.values(3, "leaf0", "title3", "abstract3", "leaf0").addBatch()
			.values(4, "leaf1", "title4", "abstract4", "leaf1").addBatch()
			.execute();
		
		insert(leafLayer)
			.columns(
				leafLayer.id,
				leafLayer.genericLayerId,
				leafLayer.datasetId)
			.values(0, 3, vectorDatasetId).addBatch()
			.values(1, 4, vectorDatasetId).addBatch()
			.execute();
		
		insert(layerStructure)
			.columns(
				layerStructure.parentLayerId,
				layerStructure.childLayerId,
				layerStructure.layerOrder)
			.values(0, 1, 0).addBatch()
			.values(0, 2, 1).addBatch()
			.values(1, 2, 0).addBatch()
			.values(1, 3, 1).addBatch()
			.values(2, 1, 0).addBatch()			
			.values(2, 4, 1).addBatch()
			.execute();
		
		insert(service)
			.set(service.id, 0)
			.set(service.genericLayerId, 0)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
			
		Failure failure = f.ask(serviceManager, new GetService("service"), Failure.class).get();
		Throwable cause = failure.getCause();
		assertNotNull(cause);
		
		String message = cause.getMessage();
		assertNotNull(message);
		assertTrue(message.contains("cycle"));
		assertTrue(message.contains("group1"));
		
		failure = f.ask(serviceManager, new GetGroupLayer("service"), Failure.class).get();
		cause = failure.getCause();
		assertNotNull(cause);
		
		message = cause.getMessage();
		assertNotNull(message);
		assertTrue(message.contains("cycle"));
		assertTrue(message.contains("group1"));
	}
	
	@Test
	public void testPreventCycle() throws Exception {
		LoggingAdapter log = nl.idgis.publisher.utils.Logging.getLogger();
		FutureUtils f = new FutureUtils(system);
		AsyncDatabaseHelper db = new AsyncDatabaseHelper(database, getClass().getName(), f, log);
		
		assertEquals(
			db.transactional(tx -> {
				CompletableFuture<Optional<Integer>> layerIdFuture = 
					tx.insert(genericLayer)
						.set(genericLayer.identification, "layer")
						.set(genericLayer.name, "layer-name")
						.executeWithKey(genericLayer.id);
				
				CompletableFuture<Long> leafLayerFuture = 
					layerIdFuture.thenCompose(layerId ->
						tx.insert(leafLayer)
							.set(leafLayer.genericLayerId, layerId.get())
							.set(leafLayer.datasetId, vectorDatasetId)
							.execute());
					
				CompletableFuture<Optional<Integer>> rootIdFuture = 
					tx.insert(genericLayer)
						.set(genericLayer.identification, "root")
						.set(genericLayer.name, "root-name")
						.executeWithKey(genericLayer.id);
					
				CompletableFuture<Optional<Integer>> groupAIdFuture = 
					tx.insert(genericLayer)
						.set(genericLayer.identification, "group-a")
						.set(genericLayer.name, "group-name-a")
						.executeWithKey(genericLayer.id);
					
				CompletableFuture<Optional<Integer>> groupBIdFuture = 
					tx.insert(genericLayer)
						.set(genericLayer.identification, "group-b")
						.set(genericLayer.name, "group-name-b")
						.executeWithKey(genericLayer.id);
				
				CompletableFuture<Long> layerGroupAFuture = 
					groupAIdFuture.thenCompose(groupAId ->
					layerIdFuture.thenCompose(layerId ->					
						tx.insert(layerStructure)
							.set(layerStructure.parentLayerId, groupAId.get())
							.set(layerStructure.childLayerId, layerId.get())
							.set(layerStructure.layerOrder, 0)
							.execute()));
				
				CompletableFuture<Long> layerGroupBFuture = 
					groupBIdFuture.thenCompose(groupBId ->
					layerIdFuture.thenCompose(layerId ->
						tx.insert(layerStructure)
							.set(layerStructure.parentLayerId, groupBId.get())
							.set(layerStructure.childLayerId, layerId.get())
							.set(layerStructure.layerOrder, 1)
							.execute()));
				
				CompletableFuture<Long> groupARootFuture = 
					rootIdFuture.thenCompose(rootId ->
					groupAIdFuture.thenCompose(groupAId ->
						tx.insert(layerStructure)
							.set(layerStructure.parentLayerId, rootId.get())
							.set(layerStructure.childLayerId, groupAId.get())
							.set(layerStructure.layerOrder, 2)
							.execute()));
				
				CompletableFuture<Long> groupBRootFuture = 
					rootIdFuture.thenCompose(rootId ->
					groupBIdFuture.thenCompose(groupBId ->
						tx.insert(layerStructure)
							.set(layerStructure.parentLayerId, rootId.get())
							.set(layerStructure.childLayerId, groupBId.get())
							.set(layerStructure.layerOrder, 3)
							.execute()));
					
				CompletableFuture<Long> groupAGroupBFuture =
					groupAIdFuture.thenCompose(groupAId ->
					groupBIdFuture.thenCompose(groupBId ->
						tx.insert(layerStructure)
							.set(layerStructure.parentLayerId, groupAId.get())
							.set(layerStructure.childLayerId, groupBId.get())
							.set(layerStructure.layerOrder, 4)
							.execute()));
					
				CompletableFuture<Long> groupBGroupAFuture =
					groupAIdFuture.thenCompose(groupAId ->
					groupBIdFuture.thenCompose(groupBId ->
						tx.insert(layerStructure)
							.set(layerStructure.parentLayerId, groupBId.get())
							.set(layerStructure.childLayerId, groupAId.get())
							.set(layerStructure.layerOrder, 5)
							.execute()));
				
				return CompletableFuture.allOf(
					layerIdFuture,
					leafLayerFuture,
					rootIdFuture,
					groupAIdFuture,
					groupBIdFuture,
					layerGroupAFuture,
					layerGroupBFuture,
					groupARootFuture,
					groupBRootFuture,
					groupAGroupBFuture,
					groupBGroupAFuture).<String>thenCompose(resp ->
						f.ask(
							serviceManager, 
							new GetGroupLayer(Optional.of(tx.getTransactionRef()), "root"),
							GroupLayer.class)
								.thenApply(groupLayer -> "OK")
								.exceptionally(t -> "NOT_OK"));
			}).get(),
			"NOT_OK");
	}
	
	@Test
	public void testDuplicateGroupLeafs() throws Exception {
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
			.set(leafLayer.datasetId, vectorDatasetId)
			.execute();
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "service")
			.set(genericLayer.name, "service-name")			
			.executeWithKey(genericLayer.id);
		
		int aId = insert(genericLayer)
			.set(genericLayer.identification, "a")
			.set(genericLayer.name, "a-name")			
			.executeWithKey(genericLayer.id);
		
		int bId = insert(genericLayer)
			.set(genericLayer.identification, "b")
			.set(genericLayer.name, "b-name")			
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, aId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.childLayerId, bId)
			.set(layerStructure.layerOrder, 1)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, aId)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, bId)
			.set(layerStructure.childLayerId, aId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(service)
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Service service = f.ask(serviceManager, new GetService("service"), Service.class).get();
		
		List<LayerRef<? extends Layer>> rootLayerRefs = service.getLayers();
		assertNotNull(rootLayerRefs);
		
		Iterator<LayerRef<? extends Layer>> rootLayerRefsItr = rootLayerRefs.iterator();
		assertNotNull(rootLayerRefsItr);
		
		assertTrue(rootLayerRefsItr.hasNext());
		LayerRef<? extends Layer> aLayerRef = rootLayerRefsItr.next();		
		assertNotNull(aLayerRef);
		assertTrue(aLayerRef.isGroupRef());
		
		GroupLayer aLayer = aLayerRef.asGroupRef().getLayer();
		assertEquals("a", aLayer.getId());
		
		List<LayerRef<? extends Layer>> aLayerRefs = aLayer.getLayers();
		assertNotNull(aLayerRefs);
		
		Iterator<LayerRef<? extends Layer>> aLayerRefsItr = aLayerRefs.iterator();
		assertNotNull(aLayerRefsItr);
		
		assertTrue(aLayerRefsItr.hasNext());		
		LayerRef<? extends Layer> layerRef = aLayerRefsItr.next();
		assertNotNull(layerRef);
		assertFalse(layerRef.isGroupRef());
		
		DatasetLayer layer = layerRef.asDatasetRef().getLayer();
		assertNotNull(layer);
		assertEquals("layer", layer.getId());
		
		assertFalse(aLayerRefsItr.hasNext());
	}
	
	@Test
	public void testPublishService() throws Exception {
		int groupLayerId = insert(genericLayer)
				.set(genericLayer.identification, "group-layer")
				.set(genericLayer.name, "group-layer-name")
				.executeWithKey(genericLayer.id);
		
		int firstVectorLayerId = insert(genericLayer)
				.set(genericLayer.identification, "vector-layer-0")
				.set(genericLayer.name, "vector-layer-name-0")
				.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, firstVectorLayerId)
			.set(leafLayer.datasetId, vectorDatasetId)
			.execute();
		
		int secondVectorLayerId = insert(genericLayer)
				.set(genericLayer.identification, "vector-layer-1")
				.set(genericLayer.name, "vector-layer-name-1")
				.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, secondVectorLayerId)
			.set(leafLayer.datasetId, vectorDatasetId)
			.execute();
		
		int rasterLayerId = insert(genericLayer)
			.set(genericLayer.identification, "raster-layer")
			.set(genericLayer.name, "raster-layer-name")
			.executeWithKey(genericLayer.id);
			
		insert(leafLayer)
			.set(leafLayer.genericLayerId, rasterLayerId)
			.set(leafLayer.datasetId, rasterDatasetId)
			.execute();
		
		int rootLayerId = insert(genericLayer)
			.set(genericLayer.identification, "service")
			.set(genericLayer.name, "service-name")			
			.executeWithKey(genericLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, groupLayerId)
			.set(layerStructure.childLayerId, firstVectorLayerId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, groupLayerId)
			.set(layerStructure.childLayerId, firstVectorLayerId)
			.set(layerStructure.layerOrder, 1)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, groupLayerId)
			.set(layerStructure.childLayerId, secondVectorLayerId)
			.set(layerStructure.layerOrder, 2)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootLayerId)
			.set(layerStructure.childLayerId, groupLayerId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.parentLayerId, rootLayerId)
			.set(layerStructure.childLayerId, rasterLayerId)
			.set(layerStructure.layerOrder, 1)
			.execute();
		
		insert(service)
			.set(service.genericLayerId, rootLayerId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Service stagingService = f.ask(serviceManager, new GetService("service"), Service.class).get();
		assertEquals("service", stagingService.getId());
		
		System.out.println(JsonService.toJson(stagingService));
		
		f.ask(serviceManager, new GetPublishedService("service"), NotFound.class).get();
		
		f.ask(serviceManager, new PublishService("service", Optional.of(environmentIds.get(0))), Ack.class).get();
		
		Iterator<Tuple> publishedServiceItr = 
			query().from(publishedService)
			.list(publishedService.all()).iterator();
		
		assertTrue(publishedServiceItr.hasNext());
		
		Tuple publishedServiceTuple = publishedServiceItr.next();
		String publishedServiceContent = publishedServiceTuple.get(publishedService.content);
		assertNotNull(publishedServiceContent);
		
		Map<String, Optional<String>> metadataFileIdentifications = 
			query().from(publishedServiceDataset)
				.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
				.list(publishedServiceDataset.layerName, dataset.metadataFileIdentification)
				.stream().collect(Collectors.toMap(
					t -> t.get(publishedServiceDataset.layerName), 
					t -> Optional.ofNullable (t.get(dataset.metadataFileIdentification))));
		
		Service reconstrucedPublishedService = JsonService.fromJson(publishedServiceContent, metadataFileIdentifications);
		assertNotNull(reconstrucedPublishedService);
		assertEquals("service", reconstrucedPublishedService.getId());
		
		assertFalse(publishedServiceItr.hasNext());
		
		Iterator<Tuple> publishedServiceEnvironmentItr =
			query().from(publishedService)
				.join(service).on(service.id.eq(publishedService.serviceId))
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.join(environment).on(environment.id.eq(publishedService.environmentId))
				.orderBy(environment.identification.asc())
				.list(
					genericLayer.identification,
					environment.identification).iterator();
		
		assertTrue(publishedServiceEnvironmentItr.hasNext());
		
		Tuple publishedServiceEnvironmentTuple = publishedServiceEnvironmentItr.next();
		assertEquals("service", publishedServiceEnvironmentTuple.get(genericLayer.identification));
		assertEquals("environment0", publishedServiceEnvironmentTuple.get(environment.identification));
		
		assertFalse(publishedServiceEnvironmentItr.hasNext());
		
		// 'vector-layer-name-0' should appear once in published_service_dataset, 
		// despite being included twice in the service
		assertEquals(
			Arrays.asList(
				"service:dataset0:vector-layer-name-0",
				"service:dataset0:vector-layer-name-1",
				"service:dataset1:raster-layer-name"),
			query()
				.from(publishedServiceDataset)
				.join(service).on(service.id.eq(publishedServiceDataset.serviceId))
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
				.orderBy(
					genericLayer.identification.asc(),
					dataset.identification.asc(),
					publishedServiceDataset.layerName.asc())
				.list(
					genericLayer.identification,
					dataset.identification,
					publishedServiceDataset.layerName).stream()
						.map(t ->
							t.get(genericLayer.identification) + ":"
							+ t.get(dataset.identification) + ":"
							+ t.get(publishedServiceDataset.layerName))
						.collect(Collectors.toList()));
		
		assertEquals(vectorDatasetId, query().from(datasetView).singleResult(datasetView.datasetId).longValue());
		
		assertService(
			f.ask(serviceManager, new GetService("service"), Service.class).get(), 
			f.ask(serviceManager, new GetPublishedService("service"), Service.class).get());
	}
	
	private void assertService(Service a, Service b) {
		assertEquals(a.getId(), b.getId());
		assertLayers(a.getLayers(), b.getLayers());
	}
	
	private void assertLayers(List<LayerRef<? extends Layer>> a, List<LayerRef<? extends Layer>> b) {
		Iterator<LayerRef<? extends Layer>> aItr = a.iterator();
		Iterator<LayerRef<? extends Layer>> bItr = b.iterator();
		
		while(aItr.hasNext()) {
			assertTrue(bItr.hasNext());
			
			LayerRef<? extends Layer> aRef = aItr.next();
			LayerRef<? extends Layer> bRef = bItr.next();
			
			assertEquals(aRef.isGroupRef(), bRef.isGroupRef());
			
			if(aRef.isGroupRef()) {
				assertLayers(
					aRef.asGroupRef().getLayer().getLayers(), 
					bRef.asGroupRef().getLayer().getLayers());
			} else {
				DatasetLayer aDatasetLayer = aRef.asDatasetRef().getLayer();
				DatasetLayer bDatasetLayer = bRef.asDatasetRef().getLayer();
				
				assertEquals(aDatasetLayer.getId(), bDatasetLayer.getId());
				assertEquals(aDatasetLayer.getMetadataFileIdentification(), bDatasetLayer.getMetadataFileIdentification());
				assertEquals(aDatasetLayer.getName(), bDatasetLayer.getName());
			}
		}
		
		assertFalse(bItr.hasNext());
	}

	@Test
	public void testUndoPublishService() throws Exception {
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "service")
			.set(genericLayer.name, "service-name")			
			.executeWithKey(genericLayer.id);
		
		insert(service)
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		assertFalse(query().from(publishedService).exists());
		
		f.ask(serviceManager, new PublishService("service", Optional.of(environmentIds.get(0))), Ack.class).get();
		
		assertEquals(1, query().from(publishedService).count());
		
		f.ask(serviceManager, new PublishService("service"), Ack.class).get();
		
		assertFalse(query().from(publishedService).exists());
	}
	
	@Test
	public void testWithServiceStructure() throws Exception {
		SQLQuery structureQuery = 
			QServiceStructure.withServiceStructure(query(), AbstractQuery.parent, AbstractQuery.child)
			.from(serviceStructure);
		
		assertFalse(structureQuery.exists());
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "service")
			.set(genericLayer.name, "service-name")			
			.executeWithKey(genericLayer.id);
		
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)			
			.set(leafLayer.datasetId, vectorDatasetId)
			.executeWithKey(leafLayer.id);
		
		insert(layerStructure)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(service)
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Tuple result = structureQuery
			.singleResult(
				serviceStructure.serviceIdentification,
				serviceStructure.parentLayerIdentification,
				serviceStructure.childLayerIdentification,
				serviceStructure.datasetId);
		
		assertNotNull(result);
		assertEquals("service", result.get(serviceStructure.serviceIdentification));
		assertEquals("service", result.get(serviceStructure.parentLayerIdentification));
		assertEquals("layer", result.get(serviceStructure.childLayerIdentification));
		assertEquals(Integer.valueOf(vectorDatasetId), result.get(serviceStructure.datasetId));
	}
	
	@Test
	public void testPublishedServiceIndex() throws Exception {
		int rootId0 = insert(genericLayer)
			.set(genericLayer.identification, "service0")
			.set(genericLayer.name, "service-name0")			
			.executeWithKey(genericLayer.id);
		
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		StringWriter sw = new StringWriter();
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.transform(new DOMSource(TestStyle.getGreenSld()), new StreamResult(sw));
		
		int styleId0 = insert(style)
			.set(style.identification, "style0")
			.set(style.name, "style-name0")
			.set(style.definition, sw.toString())
			.executeWithKey(style.id);
		
		int styleId1 = insert(style)
			.set(style.identification, "style1")
			.set(style.name, "style-name1")
			.set(style.definition, sw.toString())
			.executeWithKey(style.id);
		
		int leafLayerId = insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)			
			.set(leafLayer.datasetId, vectorDatasetId)
			.executeWithKey(leafLayer.id);
		
		insert(layerStyle)
			.set(layerStyle.layerId, leafLayerId)
			.set(layerStyle.styleId, styleId0)			
			.set(layerStyle.styleOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.parentLayerId, rootId0)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(service)
			.set(service.genericLayerId, rootId0)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Map<String, ServiceIndex> publishedEnvironments = getPublishedServiceIndex();
		for(String environmentId : environmentIds) {
			ServiceIndex serviceIndex = publishedEnvironments.get(environmentId);
			assertNotNull(serviceIndex);
			assertTrue(serviceIndex.getServiceNames().isEmpty());
			assertTrue(serviceIndex.getStyleNames().isEmpty());
		}
		
		String environmentId = environmentIds.get(0);
		f.ask(serviceManager, new PublishService("service0", Optional.of(environmentId)), Ack.class).get();
		
		publishedEnvironments = getPublishedServiceIndex();
		assertEquals(3, publishedEnvironments.size());
		for(String currentEnvironmentId : environmentIds) {
			assertTrue(currentEnvironmentId + " missing", publishedEnvironments.containsKey(currentEnvironmentId));
			
			ServiceIndex serviceIndex = publishedEnvironments.get(currentEnvironmentId);
			assertNotNull(serviceIndex);
			
			if(currentEnvironmentId.equals(environmentId)) {
				assertEquals(Arrays.asList("service-name0"), serviceIndex.getServiceNames());
				assertEquals(Arrays.asList("style-name0"), serviceIndex.getStyleNames());
			} else {
				assertTrue(serviceIndex.getServiceNames().isEmpty());
				assertTrue(serviceIndex.getStyleNames().isEmpty());
			}
		}
		
		insert(layerStyle)
			.set(layerStyle.layerId, leafLayerId)
			.set(layerStyle.styleId, styleId1)			
			.set(layerStyle.styleOrder, 1)
			.execute();
		
		f.ask(serviceManager, new PublishService("service0", Optional.of("environment0")), Ack.class).get();
		
		publishedEnvironments = getPublishedServiceIndex();	
		assertTrue(publishedEnvironments.containsKey("environment0"));
		ServiceIndex serviceIndex = publishedEnvironments.get("environment0");
		assertNotNull(serviceIndex);
		
		assertEquals(Arrays.asList("service-name0"), serviceIndex.getServiceNames());
		assertEquals(new HashSet<>(Arrays.asList("style-name0", "style-name1")), new HashSet<>(serviceIndex.getStyleNames()));
		
		int rootId1 = insert(genericLayer)
			.set(genericLayer.identification, "service1")
			.set(genericLayer.name, "service-name1")
			.executeWithKey(genericLayer.id);
		
		insert(service)
			.set(service.genericLayerId, rootId1)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.parentLayerId, rootId1)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		f.ask(serviceManager, new PublishService("service1", Optional.of("environment0")), Ack.class).get();
		
		publishedEnvironments = getPublishedServiceIndex();
		assertTrue(publishedEnvironments.containsKey("environment0"));		
		serviceIndex = publishedEnvironments.get("environment0");
		assertNotNull(serviceIndex);
		
		assertEquals(new HashSet<>(Arrays.asList("service-name0", "service-name1")), new HashSet<>(serviceIndex.getServiceNames()));
		assertEquals(new HashSet<>(Arrays.asList("style-name0", "style-name1")), new HashSet<>(serviceIndex.getStyleNames()));
	}
	
	@Test
	public void testPublishServiceConfidential () throws Throwable {
		// Mark the source datasets as confidential:
		assertEquals (2, update (sourceDatasetVersion)
			.set (sourceDatasetVersion.confidential, true)
			.execute ());
		
		// Mark environment 1 as confidential:
		assertEquals (1, update (environment)
			.set (environment.confidential, true)
			.where (environment.identification.eq ("environment1"))
			.execute ());
		
		int rootId = insert(genericLayer)
			.set(genericLayer.identification, "service")
			.set(genericLayer.name, "service-name")			
			.executeWithKey(genericLayer.id);
		
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		StringWriter sw = new StringWriter();
		Transformer tns = TransformerFactory.newInstance().newTransformer();
		tns.transform(new DOMSource(TestStyle.getGreenSld()), new StreamResult(sw));
		
		int styleId0 = insert(style)
			.set(style.identification, "style0")
			.set(style.name, "style-name0")
			.set(style.definition, sw.toString())
			.executeWithKey(style.id);
		
		int leafLayerId = insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)			
			.set(leafLayer.datasetId, vectorDatasetId)
			.executeWithKey(leafLayer.id);
		
		insert(layerStyle)
			.set(layerStyle.layerId, leafLayerId)
			.set(layerStyle.styleId, styleId0)			
			.set(layerStyle.styleOrder, 0)
			.execute();
		
		insert(layerStructure)
			.set(layerStructure.childLayerId, layerId)
			.set(layerStructure.parentLayerId, rootId)
			.set(layerStructure.layerOrder, 0)
			.execute();
		
		insert(service)
			.set(service.genericLayerId, rootId)
			.set(service.wfsMetadataFileIdentification, UUID.randomUUID().toString())
			.set(service.wmsMetadataFileIdentification, UUID.randomUUID().toString())
			.execute();
		
		Service stagingService = f.ask(serviceManager, new GetService("service"), Service.class).get();
		assertEquals("service", stagingService.getId());
		assertTrue (stagingService.isConfidential ());
		
		f.ask(serviceManager, new GetPublishedService("service"), NotFound.class).get();
		
		f.ask(serviceManager, new PublishService("service", Optional.of("environment0")), Failure.class).get();
		
		f.ask(serviceManager, new PublishService("service", Optional.of("environment1")), Ack.class).get();
		
		Tuple publishedServiceTuple = 
			query().from(publishedService)
			.singleResult(publishedService.all());
		
		assertNotNull(publishedServiceTuple);
		
		String publishedServiceContent = publishedServiceTuple.get(publishedService.content);
		assertNotNull(publishedServiceContent);
		
		Map<String, Optional<String>> metadataFileIdentifications = 
			query().from(publishedServiceDataset)
				.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
				.list(publishedServiceDataset.layerName, dataset.metadataFileIdentification)
				.stream().collect(Collectors.toMap(
					t -> t.get(publishedServiceDataset.layerName), 
					t -> Optional.ofNullable (t.get(dataset.metadataFileIdentification))));
		
		Service reconstrucedPublishedService = JsonService.fromJson(publishedServiceContent, metadataFileIdentifications);
		assertNotNull(reconstrucedPublishedService);
		assertEquals("service", reconstrucedPublishedService.getId());
		
		Iterator<Tuple> publishedServiceEnvironmentItr =
			query().from(publishedService)
				.join(service).on(service.id.eq(publishedService.serviceId))
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.join(environment).on(environment.id.eq(publishedService.environmentId))
				.orderBy(environment.identification.asc())
				.list(
					genericLayer.identification,
					environment.identification).iterator();
		
		assertTrue(publishedServiceEnvironmentItr.hasNext());
		
		Tuple publishedServiceEnvironmentTuple = publishedServiceEnvironmentItr.next();
		assertEquals("service", publishedServiceEnvironmentTuple.get(genericLayer.identification));
		assertEquals("environment1", publishedServiceEnvironmentTuple.get(environment.identification));
		
		assertFalse(publishedServiceEnvironmentItr.hasNext());
		
		Service publishedService = f.ask(serviceManager, new GetPublishedService("service"), Service.class).get();
		assertEquals("service", publishedService.getId());
	}

	private Map<String, ServiceIndex> getPublishedServiceIndex() throws InterruptedException, ExecutionException {
		Map<String, ServiceIndex> publishedEnvironments = new HashMap<>();
		
		for(AskResponse<Object> resp = f.askWithSender(serviceManager, new GetPublishedServiceIndex()).get();
			resp.getMessage() instanceof Item;
			resp = f.askWithSender(resp.getSender(), new NextItem()).get()) {
			
			@SuppressWarnings("unchecked")
			Item<PublishedServiceIndex> item = (Item<PublishedServiceIndex>)resp.getMessage();
			PublishedServiceIndex publishedServiceIndex = item.getContent();
			publishedEnvironments.put(
				publishedServiceIndex.getEnvironmentId(),
				publishedServiceIndex.getServiceIndex());
		}
		
		return publishedEnvironments;
	}
	
	@Test
	public void testGetDatasetLayerRef() throws Exception {	
		int layerId = insert(genericLayer)
			.set(genericLayer.identification, "layer")
			.set(genericLayer.name, "layer-name")
			.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.set(leafLayer.genericLayerId, layerId)
			.set(leafLayer.datasetId, vectorDatasetId)
			.execute();
		
		f.ask(serviceManager, new GetDatasetLayerRef("layer"), DatasetLayerRef.class).get();
		
		insert(sourceDatasetVersion)
			.columns(
				sourceDatasetVersion.type,
				sourceDatasetVersion.confidential,
				sourceDatasetVersion.metadataConfidential,
				sourceDatasetVersion.sourceDatasetId)
			.select(new SQLSubQuery().from(dataset)
				.list(
					"UNAVAILABLE", 
					false,
					false,
					dataset.sourceDatasetId))
			.execute();
		
		f.ask(serviceManager, new GetDatasetLayerRef("layer"), DatasetLayerRef.class).get();
	}
}
