package nl.idgis.publisher.metadata;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
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
import nl.idgis.publisher.metadata.MetadataDocument.ServiceLinkage;
import nl.idgis.publisher.metadata.messages.AddDataSource;
import nl.idgis.publisher.metadata.messages.AddMetadataDocument;
import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.PublishService;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
	
final String dataSourceIdentification = "dataSourceIdentification";
	
	final String sourceDatasetIdentification = "sourceDatasetIdentification";
	
	final String sourceDatasetExternalIdentification = "sourceDatasetExternalIdentification";
	
	final String categoryIdentification = "categoryIdentification";
	
	final String datasetUuid = UUID.randomUUID().toString();
	
	final String datasetFileUuid = UUID.randomUUID().toString();
	
	final String datasetIdentification = "datasetIdentification";
	
	final String layerIdentification = "layerIdentification";
	
	final String layerName = "testLayer";
	
	final String serviceIdentification = "serviceIdentification";
	
	final String serviceName = "testService";
	
	final Set<String> environmentIdentifications =
		IntStream.range(0, 5)
			.mapToObj(i -> "environmentIdentification" + i)
			.collect(Collectors.toSet());
	
	ActorRef metadataGenerator, harvester;
	
	Path serviceMetadataSourceDirectory, serviceMetadataTargetDirectory, datasetMetadataTargetDirectory;
	
	ActorRef metadataTarget;
	
	@Before
	public void actor() throws Exception {
		harvester = actorOf(HarvesterMock.props(), "harvester");
		
		FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
		
		serviceMetadataSourceDirectory = fileSystem.getPath("/service-metadata-source");
		Files.createDirectory(serviceMetadataSourceDirectory);
		
		serviceMetadataTargetDirectory = fileSystem.getPath("/service-metadata-target");
		Files.createDirectory(serviceMetadataTargetDirectory);
		
		datasetMetadataTargetDirectory = fileSystem.getPath("/dataset-metadata-target");
		Files.createDirectory(datasetMetadataTargetDirectory);
				
		metadataTarget = actorOf(
			MetadataTarget.props(
				serviceMetadataTargetDirectory, 
				datasetMetadataTargetDirectory), 
			"metadata-target");
		
		metadataGenerator = actorOf(
			MetadataGenerator.props(
				database, 
				actorOf(MetadataSource.props(harvester, serviceMetadataSourceDirectory), "metadata-source")), 
			"metadata-generator");
		
		// Prepare database
		final int dataSourceId =
			insert(dataSource)
				.columns(
					dataSource.identification,
					dataSource.name)
				.values(
					dataSourceIdentification,
					"testDataSource")
				.executeWithKey(dataSource.id);
		
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
	}
	
	@Test
	public void testGenerateMetadata() throws Exception {
		ActorRef dataSource = actorOf(DataSourceMock.props(), "dataSource");
		
		f.ask(harvester, new AddDataSource(dataSourceIdentification, dataSource), Ack.class).get();
		
		f.ask(
			dataSource, 
			new AddMetadataDocument(
				sourceDatasetExternalIdentification, 
				MetadataDocumentTest.getDocument("dataset_metadata.xml")),
			Ack.class).get();
		
		IOUtils.copy(
			getClass().getResourceAsStream("service_metadata.xml"),
			Files.newOutputStream(serviceMetadataSourceDirectory.resolve(serviceIdentification + ".xml")));

		String environmentIdentification = environmentIdentifications.iterator().next();
		String prefix = "http://" + environmentIdentification + ".example.com/";
		
		f.ask(
			metadataGenerator, 
			new GenerateMetadata(
				environmentIdentification, 
				metadataTarget,
				prefix + "geoserver/",
				prefix + "metadata/dataset/"), 
			Ack.class).get();
		
		Path wmsServiceMetadataPath = serviceMetadataTargetDirectory.resolve(serviceIdentification + "-wms.xml");
		Path wfsServiceMetadataPath = serviceMetadataTargetDirectory.resolve(serviceIdentification + "-wfs.xml");
		Path datasetMetadataPath = datasetMetadataTargetDirectory.resolve(datasetIdentification + ".xml");
		
		assertTrue(Files.exists(wmsServiceMetadataPath));
		assertTrue(Files.exists(wfsServiceMetadataPath));
		assertTrue(Files.exists(datasetMetadataPath));
		
		MetadataDocumentFactory mdf = new MetadataDocumentFactory();
		MetadataDocument datasetMetadata = mdf.parseDocument(Files.readAllBytes(datasetMetadataPath));
		
		Map<String, ServiceLinkage> serviceLinkage =
			datasetMetadata.getServiceLinkage().stream()
				.collect(Collectors.toMap(
					ServiceLinkage::getProtocol,
					Function.identity()));
		
		assertTrue(serviceLinkage.containsKey("OGC:WMS"));		
		ServiceLinkage wmsServiceLinkage = serviceLinkage.get("OGC:WMS");
		assertEquals(layerName, wmsServiceLinkage.getName());
		assertEquals(prefix + "geoserver/" + serviceName + "/wms", wmsServiceLinkage.getURL());
		
		assertTrue(serviceLinkage.containsKey("OGC:WFS"));
		ServiceLinkage wfsServiceLinkage = serviceLinkage.get("OGC:WFS");
		assertEquals(layerName, wfsServiceLinkage.getName());
		assertEquals(prefix + "geoserver/" + serviceName + "/wfs", wfsServiceLinkage.getURL());
	}
	
	@Test
	public void testKeepMetadata() throws Exception {
		String environmentIdentification = environmentIdentifications.iterator().next();
		String prefix = "http://" + environmentIdentification + ".example.com/";
		
		Path wfsMetadata = serviceMetadataTargetDirectory.resolve(serviceIdentification + "-wfs.xml");
		
		IOUtils.copy(
			getClass().getResourceAsStream("service_metadata.xml"),
			Files.newOutputStream(wfsMetadata));
		
		Path wmsMetadata = serviceMetadataTargetDirectory.resolve(serviceIdentification + "-wms.xml");
		
		IOUtils.copy(
			getClass().getResourceAsStream("service_metadata.xml"),
			Files.newOutputStream(wmsMetadata));
		
		Path datasetMetadata = datasetMetadataTargetDirectory.resolve(datasetIdentification + ".xml");
		
		IOUtils.copy(
			getClass().getResourceAsStream("dataset_metadata.xml"),
			Files.newOutputStream(datasetMetadata));
		
		f.ask(
			metadataGenerator, 
			new GenerateMetadata(
				environmentIdentification, 
				metadataTarget,
				prefix + "geoserver/",
				prefix + "metadata/dataset/"), 
			Ack.class).get();
		
		assertTrue(Files.exists(wfsMetadata));
		assertTrue(Files.exists(wmsMetadata));
		assertTrue(Files.exists(datasetMetadata));
	}
}
