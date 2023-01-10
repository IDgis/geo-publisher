package controllers;

import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedServiceKeyword.publishedServiceKeyword;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;

import nl.idgis.dav.model.DefaultResource;
import nl.idgis.dav.model.DefaultResourceDescription;
import nl.idgis.dav.model.DefaultResourceProperties;
import nl.idgis.dav.model.Resource;
import nl.idgis.dav.model.ResourceDescription;
import nl.idgis.dav.model.ResourceProperties;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocument.Topic;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.xml.exceptions.NotFound;
import play.libs.Json;
import util.MetadataConfig;
import util.QueryDSL;
import util.Security;
import util.QueryDSL.Transaction;

public class ServiceMetadata extends AbstractMetadata {
	
	private static final String ENDPOINT_CODE_LIST_VALUE = "WebServices";

	private static final String ENDPOINT_CODE_LIST = "http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList";

	private static final String ENDPOINT_OPERATION_NAME = "GetCapabilitities";
		
	private final MetadataDocument template;
	
	@Inject
	public ServiceMetadata(MetadataConfig config, QueryDSL q, Security s) throws Exception {
		this(config, q, s, getTemplate(), "/");
	}
	
	public ServiceMetadata(MetadataConfig config, QueryDSL q, Security s, MetadataDocument template, String prefix) {
		super(config, q, s, prefix);
		
		this.template = template;
	}
	
	private static MetadataDocument getTemplate() throws Exception {
		MetadataDocumentFactory mdf = new MetadataDocumentFactory();
		
		return mdf.parseDocument(
			ServiceMetadata.class
				.getClassLoader()
				.getResourceAsStream("nl/idgis/publisher/metadata/service_metadata.xml"));
	}
	
	@Override
	public ServiceMetadata withPrefix(String prefix) {
		return new ServiceMetadata(config, q, s, template, prefix);
	}
	
	private SQLQuery fromService(Transaction tx) {
		SQLQuery query = tx.query().from(service)
			.join(publishedService).on(publishedService.serviceId.eq(service.id))
			.join(environment).on(environment.id.eq(publishedService.environmentId));
		
		if(!s.isTrusted()) {
			query.where(environment.confidential.isFalse());
		}
		
		return query;
	}

	@Override
	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
			
			Tuple serviceTuple = fromService(tx)
				.join(constants).on(constants.id.eq(service.constantsId))
				.where(service.wmsMetadataFileIdentification.eq(id)
					.or(service.wfsMetadataFileIdentification.eq(id)))
				.singleResult(
					service.id,
					publishedService.createTime,
					publishedService.content,
					publishedService.title,
					publishedService.alternateTitle,
					publishedService.abstractCol,
					constants.contact,
					constants.organization,
					constants.position,
					constants.addressType,
					constants.address,
					constants.city,
					constants.state,
					constants.zipcode,
					constants.country,
					constants.telephone,
					constants.fax,
					constants.email,
					service.wmsMetadataFileIdentification,
					service.wfsMetadataFileIdentification,
					environment.confidential,
					environment.url,
					environment.wmsOnly);
			
			if(serviceTuple == null) {
				return Optional.<Resource>empty();
			}
			
			JsonNode serviceInfo = Json.parse(serviceTuple.get(publishedService.content));
			boolean environmentWmsOnly = serviceTuple.get(environment.wmsOnly);
			
			if(id.equals(serviceTuple.get(service.wfsMetadataFileIdentification)) && environmentWmsOnly) {
				return Optional.<Resource>empty();
			}
			
			int serviceId = serviceTuple.get(service.id);
			
			List<String> keywords = tx.query().from(publishedServiceKeyword)
				.where(publishedServiceKeyword.serviceId.eq(serviceId))
				.orderBy(publishedServiceKeyword.keyword.asc())
				.list(publishedServiceKeyword.keyword);
			
			SQLQuery serviceDatasetQuery = tx.query().from(publishedServiceDataset)
				.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
				.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId));
			
			if(!s.isTrusted()) {
				// do not generate links to confidential (= inaccessible) metadata documents.
				
				serviceDatasetQuery
					.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
					.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
							.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
							.list(sourceDatasetVersion.id.max())))
					.where(sourceDatasetVersion.metadataConfidential.isFalse());
			}
			
			List<Tuple> serviceDatasetTuples = serviceDatasetQuery
				.where(publishedServiceDataset.serviceId.eq(serviceId)
					.and(sourceDataset.deleteTime.isNull()))
				.orderBy(dataset.id.asc(), publishedServiceDataset.layerName.asc())
				.list(
					dataset.id,
					dataset.metadataIdentification,
					dataset.metadataFileIdentification,
					publishedServiceDataset.layerName);
			
			MetadataDocument metadataDocument = template.clone();
			metadataDocument.setFileIdentifier(id);
			
			metadataDocument.setServiceTitle(serviceTuple.get(publishedService.title));
			metadataDocument.setServiceAlternateTitle(serviceTuple.get(publishedService.alternateTitle));
			metadataDocument.setServiceAbstract(serviceTuple.get(publishedService.abstractCol));
			
			metadataDocument.setDate(Topic.SERVICE, "publication", serviceTuple.get(publishedService.createTime));
			metadataDocument.setMetaDataCreationDate(serviceTuple.get(publishedService.createTime));
			
			metadataDocument.removeServiceKeywords();
			metadataDocument.addServiceKeywords(
				keywords, 
				"GEMET - Concepts, version 2.4", 
				"2010-01-13", 
				"http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode", 
				"publication");
			
			String role = "pointOfContact";		
			metadataDocument.setServiceResponsiblePartyName(role, serviceTuple.get(constants.organization));
			metadataDocument.setServiceResponsiblePartyEmail(role, serviceTuple.get(constants.email));		
			metadataDocument.setMetaDataPointOfContactName(role, serviceTuple.get(constants.organization));
			metadataDocument.setMetaDataPointOfContactEmail(role, serviceTuple.get(constants.email));
			
			metadataDocument.removeOperatesOn();
			for(Tuple serviceDatasetTuple : serviceDatasetTuples) {
				int datasetId = serviceDatasetTuple.get(dataset.id);
				
				// a service can operate on a dataset using multiple
				// layer names (i.e. we encounter it multiple times in this loop), 
				// but it should reported only once here.
				Integer lastDatasetId = null;
				if(lastDatasetId == null || datasetId != lastDatasetId) {
					lastDatasetId = datasetId;
					
					String fileIdentification = serviceDatasetTuple.get(dataset.metadataFileIdentification);
					String uuidref = 
							config.getMetadataUrlPrefix() + 
							"dataset/" + getName(fileIdentification) + 
							"#MD_DataIdentification";
					
					metadataDocument.addOperatesOn(uuidref);
				}
			}
			
			metadataDocument.removeServiceType();
			metadataDocument.removeServiceEndpoint();
			metadataDocument.removeBrowseGraphic();
			metadataDocument.removeServiceLinkage();
			metadataDocument.removeSVCoupledResource();
			
			ServiceType serviceType;
			if(id.equals(serviceTuple.get(service.wmsMetadataFileIdentification))) {
				serviceType = ServiceType.WMS;
			} else {
				serviceType = ServiceType.WFS;
			}
			
			// we obtain the serviceName from the published service content
			// because we don't store it anywhere else at the moment.
			String serviceName = serviceInfo.get("name").asText();
			
			String environmentUrl = serviceTuple.get(environment.url);
			
			String linkage = getServiceLinkage(environmentUrl, serviceName, serviceType);
			
			metadataDocument.addServiceType(serviceType.getName());
			metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
			
			if(!serviceDatasetTuples.isEmpty()) {
				Boolean confidential = serviceTuple.get(environment.confidential);
				
				if(confidential) {
					config.getViewerUrlSecurePrefix().ifPresent(viewerUrlPrefix -> {
						try {
							metadataDocument.addServiceLinkage(viewerUrlPrefix + "?service=" + serviceName, "landingpage", null, "accessPoint");
						} catch(NotFound nf) {
							throw new RuntimeException(nf);
						}
					});
				} else if(environmentWmsOnly) {
					config.getViewerUrlWmsOnlyPrefix().ifPresent(viewerUrlPrefix -> {
						try {
							metadataDocument.addServiceLinkage(viewerUrlPrefix + "?service=" + serviceName, "landingpage", null, "accessPoint");
						} catch(NotFound nf) {
							throw new RuntimeException(nf);
						}
					});
				} else {
					config.getViewerUrlPublicPrefix().ifPresent(viewerUrlPrefix -> {
						try {
							metadataDocument.addServiceLinkage(viewerUrlPrefix + "?service=" + serviceName, "landingpage", null, "accessPoint");
						} catch(NotFound nf) {
							throw new RuntimeException(nf);
						}
					});
				}
			}
			
			for(Tuple serviceDatasetTuple : serviceDatasetTuples) {
				int datasetId = serviceDatasetTuple.get(dataset.id);
				
				// a service can operate on a dataset using multiple
				// layer names (i.e. we encounter it multiple times in this loop), 
				// but it should reported only once here.
				Integer lastDatasetId = null;
				if(lastDatasetId == null || datasetId != lastDatasetId) {
					lastDatasetId = datasetId;
					
					String identifier = serviceDatasetTuple.get(dataset.metadataIdentification);
					String scopedName = serviceDatasetTuple.get(publishedServiceDataset.layerName);
					if(serviceType == ServiceType.WMS) {
						metadataDocument.addServiceBrowseGraphic(linkage + config.getBrowseGraphicWmsRequest() + scopedName);
					}
					metadataDocument.addServiceLinkage(linkage, serviceType.getProtocol(), scopedName, "accessPoint");
					metadataDocument.addSVCoupledResource(serviceType.getOperationName(), identifier, scopedName);
				}
			}
			
			metadataDocument.removeStylesheet();
			stylesheet("services").ifPresent(metadataDocument::setStylesheet);
			
			return Optional.<Resource>of(new DefaultResource("application/xml", metadataDocument.getContent()));
		}));
	}

	@Override
	public Stream<ResourceDescription> descriptions() {
		return q.withTransaction(tx ->
			fromService(tx)
			.list(
				publishedService.createTime,
				environment.confidential,
				service.wmsMetadataFileIdentification, 
				service.wfsMetadataFileIdentification).stream()
			.flatMap(serviceTuple -> {
				Timestamp createTime = serviceTuple.get(publishedService.createTime);
				Map<QName, String> customProperties = new HashMap<QName, String>();
				customProperties.put(
					new QName("http://idgis.nl/geopublisher", "confidential"), 
					serviceTuple.get(environment.confidential).toString());
				
				return Stream.of(
					serviceTuple.get(service.wmsMetadataFileIdentification),
					serviceTuple.get(service.wfsMetadataFileIdentification))
						.map(id ->
							new DefaultResourceDescription(getName(id), 
								new DefaultResourceProperties(false, createTime, customProperties)));
			}));
	}

	@Override
	public Optional<ResourceProperties> properties(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				Tuple serviceTuple = fromService(tx)
					.where(service.wmsMetadataFileIdentification.eq(id)
						.or(service.wfsMetadataFileIdentification.eq(id)))
					.singleResult(publishedService.createTime, environment.confidential);
				
				Timestamp createTime = serviceTuple.get(publishedService.createTime);
				Map<QName, String> customProperties = new HashMap<QName, String>();
				customProperties.put(
					new QName("http://idgis.nl/geopublisher", "confidential"), 
					serviceTuple.get(environment.confidential).toString());
				
				if(serviceTuple == null) {
					return Optional.<ResourceProperties>empty();
				} else {
					return Optional.<ResourceProperties>of(
						new DefaultResourceProperties(
							false, createTime, customProperties));
				}
		}));
	}
}
