package controllers;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.QTuple;

import nl.idgis.dav.model.Resource;
import nl.idgis.dav.model.ResourceDescription;
import nl.idgis.dav.model.ResourceProperties;

import nl.idgis.dav.model.DefaultResource;
import nl.idgis.dav.model.DefaultResourceDescription;
import nl.idgis.dav.model.DefaultResourceProperties;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.xml.exceptions.NotFound;
import play.api.mvc.Handler;
import play.api.mvc.RequestHeader;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import util.InetFilter;
import util.MetadataConfig;
import util.QueryDSL;
import util.QueryDSL.Transaction;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QEnvironment.environment;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class DatasetMetadata extends AbstractMetadata {
	
	private final static String stylesheet = "datasets/intern/metadata.xsl";
		
	private final MetadataDocumentFactory mdf;
	
	@Inject
	public DatasetMetadata(WebJarAssets webJarAssets, InetFilter filter, MetadataConfig config, QueryDSL q) throws Exception {
		this(webJarAssets, filter, config, q, new MetadataDocumentFactory(), "/");
	}
	
	public DatasetMetadata(WebJarAssets webJarAssets, InetFilter filter, MetadataConfig config, QueryDSL q, MetadataDocumentFactory mdf, String prefix) {
		super(webJarAssets, filter, config, q, prefix);
		
		this.mdf = mdf;
	}
	
	@Override
	public DatasetMetadata withPrefix(String prefix) {
		return new DatasetMetadata(webJarAssets, filter, config, q, mdf, prefix);
	}
	
	private SQLQuery fromDataset(Transaction tx) {
		SQLQuery query = tx.query().from(dataset)
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
			.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
			.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
				.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
				.list(sourceDatasetVersion.id.max())));
		
		if(!isTrusted()) {
			query.where(sourceDatasetVersion.confidential.isFalse());
		}
		
		return query;
	}
	
	@Override
	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				Tuple datasetTuple = fromDataset(tx)
					.where(dataset.metadataFileIdentification.eq(id))
					.singleResult(
						dataset.id,
						dataset.metadataIdentification,
						sourceDatasetMetadata.document);
				
				if(datasetTuple == null) {
					return Optional.<Resource>empty();
				}
				
				int datasetId = datasetTuple.get(dataset.id);
				
				List<Tuple> serviceTuples = tx.query().from(publishedService)
					.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
					.join(environment).on(environment.id.eq(publishedService.environmentId))
					.where(publishedServiceDataset.datasetId.eq(datasetId))
					.list(
						publishedService.content,
						environment.identification,
						publishedServiceDataset.layerName);
				
				MetadataDocument metadataDocument = mdf.parseDocument(datasetTuple.get(sourceDatasetMetadata.document));
				
				metadataDocument.setDatasetIdentifier(datasetTuple.get(dataset.metadataIdentification));
				metadataDocument.setFileIdentifier(id);
				
				List<String> browseGraphics = metadataDocument.getDatasetBrowseGraphics();
				
				metadataDocument.removeServiceLinkage();
				for(Tuple serviceTuple : serviceTuples) {
					JsonNode serviceInfo = Json.parse(serviceTuple.get(publishedService.content));
					
					String serviceName = serviceInfo.get("name").asText();
					String environmentId = serviceTuple.get(environment.identification);
					String scopedName = serviceTuple.get(publishedServiceDataset.layerName);
					
					config.getDownloadUrlPrefix().ifPresent(downloadUrlPrefix -> {
						try {
							metadataDocument.addServiceLinkage(downloadUrlPrefix + id, "download", serviceTuple.get(publishedServiceDataset.layerName));
						} catch(NotFound nf) {
							throw new RuntimeException(nf);
						}
					});
					
					// we only automatically generate browseGraphics 
					// when none where provided by the source. 
					if(browseGraphics.isEmpty()) {
						String linkage = getServiceLinkage(environmentId, serviceName, ServiceType.WMS);
						metadataDocument.addDatasetBrowseGraphic(linkage + BROWSE_GRAPHIC_WMS_REQUEST + scopedName);
					}
					
					for(ServiceType serviceType : ServiceType.values()) {
						String linkage = getServiceLinkage(environmentId, serviceName, serviceType);
						String protocol = serviceType.getProtocol();
						
						metadataDocument.addServiceLinkage(linkage, protocol, scopedName);
					}
				}
				
				metadataDocument.setStylesheet(routes.WebJarAssets.at(webJarAssets.locate(stylesheet)).url());
				
				return Optional.<Resource>of(new DefaultResource("application/xml", metadataDocument.getContent()));
		}));
	}

	@Override
	public Stream<ResourceDescription> descriptions() {
		return q.withTransaction(tx -> 
				fromDataset(tx)
				.list(
					dataset.metadataFileIdentification, 
					sourceDatasetVersion.revision).stream()
					.map(datasetTuple ->
						new DefaultResourceDescription(
							getName(datasetTuple.get(dataset.metadataFileIdentification)),
							new DefaultResourceProperties(
								false,
								datasetTuple.get(sourceDatasetVersion.revision)))));
	}

	@Override
	public Optional<ResourceProperties> properties(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				Tuple datasetTuple = fromDataset(tx)
					.where(dataset.metadataFileIdentification.eq(id))
					.singleResult(new QTuple(sourceDatasetVersion.revision));
				
				if(datasetTuple == null) {
					return Optional.<ResourceProperties>empty();
				} else {
					return Optional.<ResourceProperties>of(
						new DefaultResourceProperties(
							false, 
							datasetTuple.get(sourceDatasetVersion.revision)));
				}
		}));
	}
}
