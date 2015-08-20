package nl.idgis.publisher.metadata;

import org.junit.Before;
import org.junit.Test;

import com.mysema.query.sql.dml.SQLInsertClause;

import akka.actor.ActorRef;

import nl.idgis.publisher.domain.SourceDatasetType;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.metadata.messages.AddDataSource;
import nl.idgis.publisher.metadata.messages.AddMetadataDocument;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.PublishService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QEnvironment.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MetadataGeneratorTest extends AbstractServiceTest {
	
	ActorRef metadataGenerator, harvester;
	
	MetadataStoreMock serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget;
	
	ActorRef metadataTarget;
	
	@Before
	public void actor() throws Exception {
		harvester = actorOf(HarvesterMock.props(), "harvester");		
		
		serviceMetadataSource = new MetadataStoreMock(f);
		datasetMetadataTarget = new MetadataStoreMock(f);
		serviceMetadataTarget = new MetadataStoreMock(f);
		
		metadataTarget = actorOf(MetadataTarget.props(datasetMetadataTarget, serviceMetadataTarget), "metadata-target");
		
		metadataGenerator = actorOf(
			MetadataGenerator.props(
				database, 
				actorOf(MetadataSource.props(harvester, serviceMetadataSource), "metadata-source")), 
			"metadata-generator");
	}
	
	
	@Test
	public void testGenerate() throws Exception {
		final String dataSourceIdentification = "dataSourceIdentification";
		
		final int dataSourceId =
			insert(dataSource)
				.columns(
					dataSource.identification,
					dataSource.name)
				.values(
					dataSourceIdentification,
					"testDataSource")
				.executeWithKey(dataSource.id);
		
		final String sourceDatasetIdentification = "sourceDatasetIdentification";
		final String sourceDatasetExternalIdentification = "sourceDatasetExternalIdentification";
		
		final int sourceDatasetId =
			insert(sourceDataset)
				.columns(
					sourceDataset.identification,
					sourceDataset.externalIdentification,
					sourceDataset.dataSourceId)
				.values(
					sourceDatasetIdentification,
					sourceDatasetExternalIdentification,
					dataSourceId)
				.executeWithKey(sourceDataset.id);
		
		final String categoryIdentification = "categoryIdentification";
		
		int categoryId = insert(category)
				.columns(
					category.identification,
					category.name)
				.values(
					categoryIdentification, 
					"testCategory")
			.executeWithKey(category.id);
		
		final int sourceDatasetVersionId =
			insert(sourceDatasetVersion)
				.columns(
					sourceDatasetVersion.sourceDatasetId,
					sourceDatasetVersion.type,
					sourceDatasetVersion.confidential,
					sourceDatasetVersion.categoryId)
				.values(
					sourceDatasetId,
					SourceDatasetType.RASTER.toString(),
					false,
					categoryId)
				.executeWithKey(sourceDatasetVersion.id);
		
		final String datasetUuid = UUID.randomUUID().toString();
		final String datasetFileUuid = UUID.randomUUID().toString();
		final String datasetIdentification = "datasetIdentification";
		
		final int datasetId =
			insert(dataset)
				.columns(
					dataset.identification,
					dataset.name,
					dataset.uuid,
					dataset.fileUuid,
					dataset.sourceDatasetId)
				.values(
					datasetIdentification,
					"testDataset",
					datasetUuid,
					datasetFileUuid,
					sourceDatasetId)
				.executeWithKey(dataset.id);
		
		final int jobId =
			insert(job)
				.columns(
					job.type)
				.values(
					JobType.IMPORT.toString())
				.executeWithKey(job.id);
		
		insert(importJob)
			.columns(
				importJob.jobId,
				importJob.datasetId,
				importJob.sourceDatasetVersionId,
				importJob.filterConditions)
			.values(
				jobId,
				datasetId,
				sourceDatasetVersionId,
				"")
			.executeWithKey(importJob.id);
		
		insert(jobState)
			.columns(
				jobState.jobId,
				jobState.state)
			.values(jobId, JobState.STARTED.toString()).addBatch()
			.values(jobId, JobState.SUCCEEDED.toString()).addBatch()
			.execute();
		
		final String layerIdentification = "layerIdentification";		
		final String layerName = "testLayer";
		
		final int layerGenericLayerId = 
			insert(genericLayer)
				.columns(					
					genericLayer.identification,
					genericLayer.name)
				.values(
					layerIdentification,
					layerName)
				.executeWithKey(genericLayer.id);
		
		insert(leafLayer)
			.columns(
				leafLayer.genericLayerId,
				leafLayer.datasetId)
			.values(
				layerGenericLayerId,
				datasetId)
			.execute();
		
		final String serviceIdentification = "serviceIdentification";
		final String serviceName = "testService";
		
		final int serviceGenericLayerId =
			insert(genericLayer)
				.columns(
					genericLayer.identification,
					genericLayer.name)
				.values(
					serviceIdentification, 
					serviceName)
				.executeWithKey(genericLayer.id);
		
		insert(service)
			.columns(service.genericLayerId)
			.values(serviceGenericLayerId)
			.execute();
		
		insert(layerStructure)
			.columns(
				layerStructure.parentLayerId,
				layerStructure.childLayerId,
				layerStructure.layerOrder)
			.values(
				serviceGenericLayerId,
				layerGenericLayerId,
				0)
			.execute();
		
		// Verify database content
		Service service = f.ask(serviceManager, new GetService(serviceIdentification), Service.class).get();
		assertEquals(serviceIdentification, service.getId());
		assertEquals(serviceName, service.getName());
		
		List<LayerRef<? extends Layer>> layerRefs = service.getLayers();		
		assertEquals(1, layerRefs.size());
		
		LayerRef<? extends Layer> layerRef = layerRefs.get(0);
		assertFalse(layerRef.isGroupRef());
		
		DatasetLayerRef datasetLayerRef = layerRef.asDatasetRef();
		DatasetLayer datasetLayer = datasetLayerRef.getLayer();
		
		assertEquals(layerIdentification, datasetLayer.getId());
		assertEquals(layerName, datasetLayer.getName());
		
		// Publish service
		Set<String> environmentIdentifications =
			IntStream.range(0, 5)
				.mapToObj(i -> "environmentIdentification" + i)
				.collect(Collectors.toSet());
		
		SQLInsertClause environmentInsert = insert(environment)
			.columns(environment.identification);
		
		environmentIdentifications.stream()
			.forEach(environmentIdentification ->
				environmentInsert
					.values(environmentIdentification)
					.addBatch());
		
		environmentInsert.execute();
		
		f.ask(
			serviceManager, 
			new PublishService(
				serviceIdentification,
				environmentIdentifications), 
			Ack.class).get();
		
		ActorRef dataSource = actorOf(DataSourceMock.props(), "dataSource");
		
		f.ask(harvester, new AddDataSource(dataSourceIdentification, dataSource), Ack.class).get();
		
		f.ask(
			dataSource, 
			new AddMetadataDocument(
				sourceDatasetExternalIdentification, 
				MetadataDocumentTest.getDocument("dataset_metadata.xml")),
			Ack.class).get();
		
		serviceMetadataSource.put(
			serviceIdentification, 
			MetadataDocumentTest.getDocument("service_metadata.xml")).get();

		f.ask(
			metadataGenerator, 
			new GenerateMetadata(
				environmentIdentifications.iterator().next(), 
				metadataTarget), 
			Ack.class).get();
		
		serviceMetadataTarget.get(serviceIdentification + "-wms").get();
		serviceMetadataTarget.get(serviceIdentification + "-wfs").get();
		datasetMetadataTarget.get(datasetIdentification).get();
		
		assertTrue(serviceMetadataTarget.getOverwritten().isEmpty());
	}
}
