package controller;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.QTuple;
import com.typesafe.config.Config;

import model.dav.Resource;
import model.dav.ResourceDescription;
import model.dav.ResourceProperties;

import model.dav.DefaultResource;
import model.dav.DefaultResourceDescription;
import model.dav.DefaultResourceProperties;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import play.Configuration;
import play.api.mvc.Handler;
import play.api.mvc.RequestHeader;
import play.api.routing.Router;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import router.dav.SimpleWebDAV;
import util.InetFilter;
import util.MetadataConfig;
import util.QueryDSL;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QPublishedServiceKeyword.publishedServiceKeyword;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class ServiceMetadata extends AbstractMetadata {
	
	private static final String ENDPOINT_CODE_LIST_VALUE = "WebServices";

	private static final String ENDPOINT_CODE_LIST = "http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList";

	private static final String ENDPOINT_OPERATION_NAME = "GetCapabilitities";
	
	private static final String BROWSE_GRAPHIC_BASE_URL =  
			"?request=GetMap&service=WMS&SRS=EPSG:28992&CRS=EPSG:28992"
			+ "&bbox=180000,459000,270000,540000&width=600&height=662&"
			+ "format=image/png&styles=";
	
	private static final Predicate notConfidential = 
		new SQLSubQuery().from(publishedServiceEnvironment)
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(publishedServiceEnvironment.serviceId.eq(service.id))
			.where(environment.confidential.isFalse())
			.exists();
		
	private final MetadataDocument template;
	
	@Inject
	public ServiceMetadata(InetFilter filter, MetadataConfig config, QueryDSL q) throws Exception {
		this(filter, config, q, getTemplate(), "/");
	}
	
	private static MetadataDocument getTemplate() throws Exception {
		MetadataDocumentFactory mdf = new MetadataDocumentFactory();
		
		return mdf.parseDocument(
			ServiceMetadata.class
				.getClassLoader()
				.getResourceAsStream("nl/idgis/publisher/metadata/service_metadata.xml"));
	}
	
	public ServiceMetadata(InetFilter filter, MetadataConfig config, QueryDSL q, MetadataDocument template, String prefix) {
		super(filter, config, q, prefix);
		
		this.template = template;
	}
	
	@Override
	public ServiceMetadata withPrefix(String prefix) {
		return new ServiceMetadata(filter, config, q, template, prefix);
	}

	@Override
	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
			
			Tuple serviceTuple = tx.query().from(service)
				.join(publishedService).on(publishedService.serviceId.eq(service.id))
				.join(constants).on(constants.id.eq(service.constantsId))
				.where(notConfidential)
				.where(service.wmsMetadataFileIdentification.eq(id)
					.or(service.wfsMetadataFileIdentification.eq(id)))
				.singleResult(
					service.id,
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
					service.wfsMetadataFileIdentification);
			
			if(serviceTuple == null) {
				return Optional.<Resource>empty();
			}
			
			int serviceId = serviceTuple.get(service.id);
			
			List<String> keywords = tx.query().from(publishedServiceKeyword)
				.where(publishedServiceKeyword.serviceId.eq(serviceId))
				.orderBy(publishedServiceKeyword.keyword.asc())
				.list(publishedServiceKeyword.keyword);
			
			List<Tuple> serviceDatasetTuples = tx.query().from(publishedServiceDataset)
				.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
				.where(publishedServiceDataset.serviceId.eq(serviceId))
				.orderBy(dataset.id.asc(), publishedServiceDataset.layerName.asc())
				.list(
					dataset.id,
					dataset.metadataIdentification,
					dataset.metadataFileIdentification,
					publishedServiceDataset.layerName);
			
			List<String> environmentIds = tx.query().from(publishedServiceEnvironment)
				.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
				.list(environment.identification);
			
			MetadataDocument metadataDocument = template.clone();
			metadataDocument.setFileIdentifier(id);
			
			metadataDocument.setServiceTitle(serviceTuple.get(publishedService.title));
			metadataDocument.setServiceAlternateTitle(serviceTuple.get(publishedService.alternateTitle));
			metadataDocument.setServiceAbstract(serviceTuple.get(publishedService.abstractCol));
			
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
			
			Integer lastDatasetId = null;
			metadataDocument.removeOperatesOn();			
			for(Tuple serviceDatasetTuple : serviceDatasetTuples) {
				int datasetId = serviceDatasetTuple.get(dataset.id);
				
				// a service can operate on a dataset using multiple
				// layer names (i.e. we encounter it multiple times in this loop), 
				// but it should reported only once here.
				if(lastDatasetId == null || datasetId != lastDatasetId) {
					lastDatasetId = datasetId;
					
					String uuid = serviceDatasetTuple.get(dataset.metadataIdentification);
					String fileIdentification = serviceDatasetTuple.get(dataset.metadataFileIdentification);
					String uuidref = config.getUrlPrefix() + "dataset/" + getName(fileIdentification);
					
					metadataDocument.addOperatesOn(uuid, uuidref);
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
			JsonNode serviceInfo = Json.parse(serviceTuple.get(publishedService.content));
			String serviceName = serviceInfo.get("name").asText();
			
			for(String environmentId : environmentIds) {
				String linkage = getServiceLinkage(environmentId, serviceName, serviceType);
				
				metadataDocument.addServiceType(serviceType.getName());
				metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
				
				for(Tuple serviceDatasetTuple : serviceDatasetTuples) {
					String identifier = serviceDatasetTuple.get(dataset.metadataFileIdentification);
					String scopedName = serviceDatasetTuple.get(publishedServiceDataset.layerName);						
					
					if(serviceType == ServiceType.WMS) {
						metadataDocument.addBrowseGraphic(linkage + BROWSE_GRAPHIC_BASE_URL + "&layers=" + scopedName);
					}
					metadataDocument.addServiceLinkage(linkage, serviceType.getProtocol(), scopedName);
					metadataDocument.addSVCoupledResource(serviceType.getOperationName(), identifier, scopedName);
				}
			}
			
			return Optional.<Resource>of(new DefaultResource("application/xml", metadataDocument.getContent()));
		}));
	}
	
	private static class ServiceInfo {
		
		final String id;
		
		final Tuple t;
		
		ServiceInfo(String id, Tuple t) {
			this.id = id;
			this.t = t;
		}
	}

	@Override
	public Stream<ResourceDescription> descriptions() {
		return q.withTransaction(tx -> {
			
			return
				tx.query().from(service)
					.where(notConfidential)
					.list(service.wmsMetadataFileIdentification, service.wfsMetadataFileIdentification).stream()
					.flatMap(t ->
						Stream.of(
							new ServiceInfo(t.get(service.wmsMetadataFileIdentification), t),
							new ServiceInfo(t.get(service.wfsMetadataFileIdentification), t)))
					.map(info -> {
						ResourceProperties properties = new DefaultResourceProperties(false);

						return new DefaultResourceDescription(getName(info.id), properties);
					});
		});
	}

	@Override
	public Optional<ResourceProperties> properties(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				Tuple serviceTuple = tx.query().from(service)
					.where(notConfidential)
					.where(service.wmsMetadataFileIdentification.eq(id)
						.or(service.wfsMetadataFileIdentification.eq(id)))
					.singleResult();
				
				if(serviceTuple == null) {				
					return Optional.<ResourceProperties>empty();
				} else {
					return Optional.<ResourceProperties>of(new DefaultResourceProperties(false));
				}
		}));
	}
}
