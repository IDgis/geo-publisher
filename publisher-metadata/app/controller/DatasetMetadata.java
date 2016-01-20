package controller;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.QTuple;

import model.dav.Resource;
import model.dav.ResourceDescription;
import model.dav.ResourceProperties;

import model.dav.DefaultResource;
import model.dav.DefaultResourceDescription;
import model.dav.DefaultResourceProperties;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;

import play.api.mvc.Handler;
import play.api.mvc.RequestHeader;
import play.api.routing.Router;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import router.dav.SimpleWebDAV;

import util.QueryDSL;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QEnvironment.environment;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;

public class DatasetMetadata extends AbstractMetadata {
		
	private final MetadataDocumentFactory mdf;
	
	@Inject
	public DatasetMetadata(QueryDSL q) throws Exception {
		this(q, new MetadataDocumentFactory(), "/");
	}
	
	public DatasetMetadata(QueryDSL q, MetadataDocumentFactory mdf, String prefix) {
		super(q, prefix);
		
		this.mdf = mdf;
	}
	
	@Override
	public DatasetMetadata withPrefix(String prefix) {
		return new DatasetMetadata(q, mdf, prefix);
	}
	
	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				Tuple td = tx.query().from(dataset)
					.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
					.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
					.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
					.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
						.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
						.list(sourceDatasetVersion.id.max())))
					.where(sourceDatasetVersion.confidential.isFalse())
					.where(new SQLSubQuery().from(publishedServiceDataset)
						.where(publishedServiceDataset.datasetId.eq(dataset.id))
						.exists())
					.where(dataset.metadataFileIdentification.eq(id))
					.singleResult(
						dataset.id,
						dataset.identification, 
						sourceDatasetMetadata.document);
				
				if(td == null) {
					return Optional.<Resource>empty();
				}
				
				MetadataDocument metadataDocument = mdf.parseDocument(td.get(sourceDatasetMetadata.document));
				metadataDocument.removeStylesheet();
				
				metadataDocument.setDatasetIdentifier(td.get(dataset.identification));
				metadataDocument.setFileIdentifier(id);
				
				int datasetId = td.get(dataset.id);
				List<Tuple> ltpsd = tx.query().from(publishedService)
					.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))					
					.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
					.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
					.list(
						publishedService.content,
						environment.identification,
						publishedServiceDataset.layerName);
				
				metadataDocument.removeServiceLinkage();
				for(Tuple tpsd : ltpsd) {
					JsonNode serviceInfo = Json.parse(tpsd.get(publishedService.content));
					
					for(ServiceType serviceType : ServiceType.values()) {
						String environmentId = tpsd.get(environment.identification);
						String serviceName = serviceInfo.get("name").asText();						
						String linkage = getServiceLinkage(environmentId, serviceName, serviceType);
						String protocol = serviceType.getProtocol();
						
						metadataDocument.addServiceLinkage(linkage, protocol, tpsd.get(publishedServiceDataset.layerName));
					}
				}
				
				return Optional.<Resource>of(new DefaultResource("application/xml", metadataDocument.getContent()));
		}));
	}

	@Override
	public Stream<ResourceDescription> descriptions() {
		return q.withTransaction(tx -> {
			
			return
				tx.query().from(dataset)
				.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
				.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
				.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
				.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
					.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
					.list(sourceDatasetVersion.id.max())))
				.where(sourceDatasetVersion.confidential.isFalse())
				.list(dataset.metadataFileIdentification).stream()
					.map(id -> {
						ResourceProperties properties = new DefaultResourceProperties(false);

						return new DefaultResourceDescription(getName(id), properties); 
					});
		});
	}

	@Override
	public Optional<ResourceProperties> properties(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				if(tx.query().from(dataset)
					.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
					.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
					.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
					.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
						.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
						.list(sourceDatasetVersion.id.max())))
					.where(sourceDatasetVersion.confidential.isFalse())
					.where(dataset.metadataFileIdentification.eq(id))
					.exists()) {
						
					return Optional.<ResourceProperties>of(new DefaultResourceProperties(false));
				} else {
					return Optional.<ResourceProperties>empty();
				}
		}));
	}
}
