package controller;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
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
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import router.dav.SimpleWebDAV;
import util.InetFilter;
import util.MetadataConfig;
import util.QueryDSL;
import util.QueryDSL.Transaction;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QEnvironment.environment;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class DatasetMetadata extends AbstractMetadata {
		
	private final MetadataDocumentFactory mdf;
	
	@Inject
	public DatasetMetadata(InetFilter filter, MetadataConfig config, QueryDSL q) throws Exception {
		this(filter, config, q, new MetadataDocumentFactory(), "/");
	}
	
	public DatasetMetadata(InetFilter filter, MetadataConfig config, QueryDSL q, MetadataDocumentFactory mdf, String prefix) {
		super(filter, config, q, prefix);
		
		this.mdf = mdf;
	}
	
	@Override
	public DatasetMetadata withPrefix(String prefix) {
		return new DatasetMetadata(filter, config, q, mdf, prefix);
	}
	
	private SQLQuery fromDataset(Transaction tx) {
		SQLQuery query = tx.query().from(dataset)
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
			.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
			.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
				.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
				.list(sourceDatasetVersion.id.max())))
			.where(new SQLSubQuery().from(publishedServiceDataset)
				.where(publishedServiceDataset.datasetId.eq(dataset.id))
				.exists());
		
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
						dataset.identification, 
						sourceDatasetMetadata.document);
				
				if(datasetTuple == null) {
					return Optional.<Resource>empty();
				}
				
				int datasetId = datasetTuple.get(dataset.id);
				
				List<Tuple> serviceTuples = tx.query().from(publishedService)
					.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))					
					.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
					.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
					.where(publishedServiceDataset.datasetId.eq(datasetId))
					.list(
						publishedService.content,
						environment.identification,
						publishedServiceDataset.layerName);
				
				MetadataDocument metadataDocument = mdf.parseDocument(datasetTuple.get(sourceDatasetMetadata.document));
				metadataDocument.removeStylesheet();
				
				metadataDocument.setDatasetIdentifier(datasetTuple.get(dataset.metadataIdentification));
				metadataDocument.setFileIdentifier(id);
				
				metadataDocument.removeServiceLinkage();
				for(Tuple serviceTuple : serviceTuples) {
					JsonNode serviceInfo = Json.parse(serviceTuple.get(publishedService.content));
					
					String serviceName = serviceInfo.get("name").asText();
					String environmentId = serviceTuple.get(environment.identification);
					
					for(ServiceType serviceType : ServiceType.values()) {
						String linkage = getServiceLinkage(environmentId, serviceName, serviceType);
						String protocol = serviceType.getProtocol();
						
						metadataDocument.addServiceLinkage(linkage, protocol, serviceTuple.get(publishedServiceDataset.layerName));
					}
				}
				
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
