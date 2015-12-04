package nl.idgis.publisher.metadata;

import static nl.idgis.publisher.database.QCategory.category;
import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QJobState.jobState;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QServiceKeyword.serviceKeyword;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.HashBiMap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.mysema.query.sql.dml.SQLInsertClause;

import akka.actor.ActorRef;
import nl.idgis.publisher.AbstractServiceTest;
import nl.idgis.publisher.domain.SourceDatasetType;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.JobType;
import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.metadata.MetadataDocument.Keywords;
import nl.idgis.publisher.metadata.MetadataDocument.ServiceLinkage;
import nl.idgis.publisher.metadata.messages.AddDataSource;
import nl.idgis.publisher.metadata.messages.AddMetadataDocument;
import nl.idgis.publisher.metadata.messages.BeginMetadataUpdate;
import nl.idgis.publisher.metadata.messages.CommitMetadata;
import nl.idgis.publisher.metadata.messages.GenerateMetadataFactory;
import nl.idgis.publisher.metadata.messages.KeepMetadata;
import nl.idgis.publisher.metadata.messages.MetadataType;
import nl.idgis.publisher.metadata.messages.UpdateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyAckRecorder;
import nl.idgis.publisher.recorder.Recording;
import nl.idgis.publisher.recorder.messages.GetRecording;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.PublishService;

public class MetadataGeneratorTest extends AbstractServiceTest {
	
	final String dataSourceIdentification = "dataSourceIdentification";
	
	final String sourceDatasetIdentification = "sourceDatasetIdentification";
	
	final String sourceDatasetExternalIdentification = "sourceDatasetExternalIdentification";
	
	final String categoryIdentification = "categoryIdentification";
	
	final String datasetMetadataIdentification = UUID.randomUUID().toString();
	
	final String datasetMetadataFileIdentification = UUID.randomUUID().toString();
	
	final String datasetIdentification = "datasetIdentification";
	
	final String layerIdentification = "layerIdentification";
	
	final String layerName = "testLayer";
	
	final String serviceIdentification = "serviceIdentification";
	
	final String serviceName = "testService";
	
	final String serviceTitle = "testServiceTitle";
	
	final String serviceAlternateTitle = "testServiceAlternateTitle";
	
	final String serviceAbstract = "testServiceAbstract";
	
	final Set<String> serviceKeywords =
		IntStream.range(0, 10)
			.mapToObj(i -> "keyword" + i)
			.collect(Collectors.toSet());
	
	final Set<String> environmentIdentifications =
		IntStream.range(0, 5)
			.mapToObj(i -> "environmentIdentification" + i)
			.collect(Collectors.toSet());
	
	ActorRef metadataGenerator, harvester;
	
	Path serviceMetadataTargetDirectory, datasetMetadataTargetDirectory;
	
	ActorRef metadataTarget;

	private HashBiMap<String, String> namespaces;
	
	@Before
	public void setUp() throws Exception {
		harvester = actorOf(HarvesterMock.props(), "harvester");
		
		FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
		
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
				actorOf(MetadataSource.props(harvester), "metadata-source")), 
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
					dataset.sourceDatasetId)
				.values(
					datasetIdentification,
					"testDataset",					
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
		
		try(Statement stmt = statement();) {
			stmt.execute("create schema staging_data");
			stmt.execute("create table staging_data." + datasetIdentification + "()");
		}
		
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
					genericLayer.name,
					genericLayer.title,
					genericLayer.abstractCol)
				.values(
					serviceIdentification, 
					serviceName,
					serviceTitle,
					serviceAbstract)
				.executeWithKey(genericLayer.id);
		
		final int constantsId = insert(constants)
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
		
		final int serviceId = insert(service)
			.columns(
				service.constantsId,
				service.genericLayerId,
				service.alternateTitle)
			.values(
				constantsId,
				serviceGenericLayerId,
				serviceAlternateTitle)
			.executeWithKey(service.id);
		
		serviceKeywords.forEach(keyword ->
			insert(serviceKeyword)
				.columns(
					serviceKeyword.keyword,
					serviceKeyword.serviceId)
				.values(
					keyword,
					serviceId)
				.execute());
		
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
		
		namespaces = HashBiMap.create();
		namespaces.put("gmd", "http://www.isotc211.org/2005/gmd");
		namespaces.put("gco", "http://www.isotc211.org/2005/gco");
		namespaces.put("srv", "http://www.isotc211.org/2005/srv");
		namespaces.put("xlink", "http://www.w3.org/1999/xlink");
		namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	}
	
	private Stream<Path> assertDocumentsExist(Stream<String> fileIdentifiers, Path target) {
		return fileIdentifiers
			.map(fileIdentification -> {
				String fileName = fileIdentification + ".xml";
				Path path = target.resolve(fileName);
				assertTrue("expected: file " + fileName, Files.exists(path));
				
				return path;
			});
	}
	
	@Test
	public void testGenerateMetadata() throws Exception {
		// assert that metadata info is not present yet
		assertFalse(
			query().from(dataset)
				.where(dataset.metadataIdentification.isNotNull()
					.or(dataset.metadataFileIdentification.isNotNull()))
				.exists());
		
		assertFalse(
			query().from(service)
				.where(service.wmsMetadataFileIdentification.isNotNull()
					.or(service.wfsMetadataFileIdentification.isNotNull()))
				.exists());
		
		ActorRef dataSource = actorOf(DataSourceMock.props(), "dataSource");
		
		f.ask(harvester, new AddDataSource(dataSourceIdentification, dataSource), Ack.class).get();
		
		f.ask(
			dataSource, 
			new AddMetadataDocument(
				sourceDatasetExternalIdentification, 
				MetadataDocumentTest.getDocument("dataset_metadata.xml")),
			Ack.class).get();

		String environmentIdentification = environmentIdentifications.iterator().next();
		String prefix = "http://" + environmentIdentification + ".example.com/";
		
		String serviceLinkagePrefix = prefix + "geoserver/";
		String datasetMetadataPrefix = prefix + "metadata/dataset/";
		
		f.ask(
			metadataGenerator,
			GenerateMetadataFactory.start()
				.environment(
					environmentIdentification, 
					metadataTarget, 
					serviceLinkagePrefix, 
					datasetMetadataPrefix)				
				.create(),
			Ack.class).get();
		
		MetadataDocumentFactory mdf = new MetadataDocumentFactory();
		
		assertEquals(
			1, 
			assertDocumentsExist(
				query().from(service)
					.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))					
					.where(genericLayer.identification.eq(serviceIdentification))
					.list(service.wmsMetadataFileIdentification).stream(), 
				serviceMetadataTargetDirectory)
					.count());
		
		assertEquals(
			1, 
			assertDocumentsExist(
				query().from(service)
					.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))					
					.where(genericLayer.identification.eq(serviceIdentification))
					.list(service.wfsMetadataFileIdentification).stream(), 
				serviceMetadataTargetDirectory)
					.map(metadataFile ->assertServiceMetadataDocument(datasetMetadataPrefix, mdf, metadataFile))
					.count());
		
		assertEquals(
			1,
			assertDocumentsExist(
				query().from(dataset)					
					.where(dataset.identification.eq(datasetIdentification))
					.list(dataset.metadataFileIdentification).stream(), 
				datasetMetadataTargetDirectory)
					.map(metadataFile -> {
						try {
							MetadataDocument datasetMetadata = mdf.parseDocument(Files.readAllBytes(metadataFile));
							
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

							// Assert that the correct schemaLocation is set:
							final String schemaLocation = datasetMetadata.xmlDocument.getString (namespaces, "/gmd:MD_Metadata/@xsi:schemaLocation");
							assertSchemaLocation (schemaLocation,
									"http://www.isotc211.org/2005/gmd", "http://schemas.opengis.net/iso/19139/20060504/gmd/gmd.xsd");
						} catch(Throwable e) {
							throw new RuntimeException(e);
						}
						
						return metadataFile;
					})
					.count());
		
		// assert if metadata info is generated
		assertTrue(
			query().from(dataset)
				.where(dataset.metadataIdentification.isNotNull()
					.or(dataset.metadataFileIdentification.isNotNull()))
				.exists());
			
		assertTrue(
			query().from(service)
				.where(service.wmsMetadataFileIdentification.isNotNull()
					.or(service.wfsMetadataFileIdentification.isNotNull()))
				.exists());
		
		f.ask(
			serviceManager, 
			new PublishService(
				serviceIdentification, 
				Collections.emptySet()), 
			Ack.class).get();
	}
	
	private static void assertSchemaLocation (final String schemaLocation, final String ... values) {
		// Parse the schemaLocation string:
		final List<String> parts = Arrays
				.stream (schemaLocation.split ("\\s+"))
				.filter (s -> !s.isEmpty ())
				.collect (Collectors.toList ());
		
		if (parts.size () % 2 != 0) {
			fail (String.format ("Invalid schemaLocation \"%s\": should have an even number of elements.", schemaLocation));
		}
		
		final Set<Pair<String, String>> schemaLocationSet = new HashSet<> ();
		for (int i = 0; i < parts.size (); i += 2) {
			schemaLocationSet.add (Pair.of (parts.get (i), parts.get (i + 1)));
		}
		
		for (int i = 0; i < values.length; i += 2) {
			final Pair<String, String> entry = Pair.of (values[i], values[i + 1]);
			
			if (!schemaLocationSet.contains (entry)) {
				fail (String.format ("Missing schemaLocation entry: %s %s (%s)", entry.getLeft (), entry.getRight (), schemaLocationSet.toString ()));
			}
		}
	}

	private Path assertServiceMetadataDocument(String datasetMetadataPrefix, MetadataDocumentFactory mdf, Path metadataFile) {
		try {
			
			byte[] metadataBytes = Files.readAllBytes(metadataFile);
			System.out.println(new String(metadataBytes, "utf-8"));
			MetadataDocument serviceMetadata = mdf.parseDocument(metadataBytes);
			
			assertEquals(serviceTitle, serviceMetadata.getServiceTitle());
			assertEquals(serviceAlternateTitle, serviceMetadata.getServiceAlternateTitle());
			assertEquals(serviceAbstract, serviceMetadata.getServiceAbstract());
			
			List<Keywords> keywords = serviceMetadata.getServiceKeywords();
			assertNotNull(keywords);
			assertEquals(1, keywords.size());			
			assertTrue(keywords.get(0).getKeywords().containsAll(serviceKeywords));
			
			assertEquals(
				1,
				serviceMetadata.getOperatesOn().stream()
					.filter(operatesOn -> {
						String uuidref = operatesOn.getUuidref();
						String href = operatesOn.getHref();
						
						return query().from(dataset)
							.where(dataset.metadataIdentification.eq(uuidref)
								.and(dataset.metadataFileIdentification
									.prepend(datasetMetadataPrefix)
									.append(".xml")
									.eq(href)))
							.exists();
					})
					.count());

			
			// Assert that the schemalocations are set:
			final String schemaLocation = serviceMetadata.xmlDocument.getString (namespaces, "/gmd:MD_Metadata/@xsi:schemaLocation");
			assertSchemaLocation (schemaLocation,
					"http://www.isotc211.org/2005/gmd", "http://schemas.opengis.net/csw/2.0.2/profiles/apiso/1.0.0/apiso.xsd",
					"http://www.isotc211.org/2005/gmd", "http://schemas.opengis.net/iso/19139/20060504/gmd/gmd.xsd");
		} catch (AssertionError e) {
			throw e;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		return metadataFile;
	}
	
	private void assertUpdateMetadata(UpdateMetadata msg) {
		String id = msg.getId();
		
		assertEquals(MetadataType.SERVICE, msg.getType());
		assertTrue("expected service metadata: " + id, query().from(service)					
			.where(service.wmsMetadataFileIdentification.eq(id)
				.or(service.wfsMetadataFileIdentification.eq(id)))
			.exists());
	}
	
	private void assertKeepMetadata(KeepMetadata msg) {
		String id = msg.getId();
		
		assertEquals(MetadataType.DATASET, msg.getType());
		assertTrue("expected dataset metadata: " + id, query().from(dataset)					
			.where(dataset.metadataFileIdentification.eq(id))
			.exists());		
	}
	
	@Test
	public void testKeepMetadata() throws Exception {
		String environmentIdentification = environmentIdentifications.iterator().next();
		String prefix = "http://" + environmentIdentification + ".example.com/";
		
		ActorRef metadataTargetMock = actorOf(AnyAckRecorder.props(new Ack()), "metadata-target-mock");
		
		f.ask(
			metadataGenerator,
			GenerateMetadataFactory.start()
				.environment(
					environmentIdentification, 
					metadataTargetMock,
					prefix + "geoserver/",
					prefix + "metadata/dataset/")
				.create(), 
			Ack.class).get();
		
		f.ask(metadataTargetMock, new GetRecording(), Recording.class).get()
			.assertNext(BeginMetadataUpdate.class)
			.assertNext(KeepMetadata.class, this::assertKeepMetadata)
			.assertNext(UpdateMetadata.class, this::assertUpdateMetadata)
			.assertNext(UpdateMetadata.class, this::assertUpdateMetadata)			
			.assertNext(CommitMetadata.class)
			.assertNotHasNext();
	}
}
