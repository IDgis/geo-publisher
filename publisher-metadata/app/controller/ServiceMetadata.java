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
	
	private static final Predicate notConfidential = 
		new SQLSubQuery().from(publishedServiceEnvironment)
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(publishedServiceEnvironment.serviceId.eq(service.id))
			.where(environment.confidential.isFalse())
			.exists();
		
	private final MetadataDocument template;
	
	@Inject
	public ServiceMetadata(MetadataConfig config, QueryDSL q) throws Exception {
		this(config, q, getTemplate(), "/");
	}
	
	private static MetadataDocument getTemplate() throws Exception {
		MetadataDocumentFactory mdf = new MetadataDocumentFactory();
		
		return mdf.parseDocument(
			ServiceMetadata.class
				.getClassLoader()
				.getResourceAsStream("nl/idgis/publisher/metadata/service_metadata.xml"));
	}
	
	public ServiceMetadata(MetadataConfig config, QueryDSL q, MetadataDocument template, String prefix) {
		super(config, q, prefix);
		
		this.template = template;
	}
	
	@Override
	public ServiceMetadata withPrefix(String prefix) {
		return new ServiceMetadata(config, q, template, prefix);
	}

	@Override
	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
			
			Tuple ts = tx.query().from(service)
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
			
			if(ts == null) {
				return Optional.<Resource>empty();
			}
			
			int serviceId = ts.get(service.id);
			
			MetadataDocument metadataDocument = template.clone();
			metadataDocument.setFileIdentifier(id);
			
			metadataDocument.setServiceTitle(ts.get(publishedService.title));
			metadataDocument.setServiceAlternateTitle(ts.get(publishedService.alternateTitle));
			metadataDocument.setServiceAbstract(ts.get(publishedService.abstractCol));
						
			List<String> keywords = tx.query().from(publishedServiceKeyword)
				.where(publishedServiceKeyword.serviceId.eq(serviceId))
				.orderBy(publishedServiceKeyword.keyword.asc())
				.list(publishedServiceKeyword.keyword);
			
			metadataDocument.removeServiceKeywords();
			metadataDocument.addServiceKeywords(
				keywords, 
				"GEMET - Concepts, version 2.4", 
				"2010-01-13", 
				"http://www.isotc211.org/2005/resources/codeList.xml#CI_DateTypeCode", 
				"publication");
			
			String role = "pointOfContact";		
			metadataDocument.setServiceResponsiblePartyName(role, ts.get(constants.organization));
			metadataDocument.setServiceResponsiblePartyEmail(role, ts.get(constants.email));		
			metadataDocument.setMetaDataPointOfContactName(role, ts.get(constants.organization));
			metadataDocument.setMetaDataPointOfContactEmail(role, ts.get(constants.email));
			
			List<Tuple> ltpsd = tx.query().from(publishedServiceDataset)
				.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
				.where(publishedServiceDataset.serviceId.eq(serviceId))
				.orderBy(dataset.id.asc(), publishedServiceDataset.layerName.asc())
				.list(
					dataset.id,
					dataset.metadataIdentification,
					dataset.metadataFileIdentification,
					publishedServiceDataset.layerName);
			
			metadataDocument.removeOperatesOn();
			Integer lastDatasetId = null;
			for(Tuple tpsd : ltpsd) {
				int datasetId = tpsd.get(dataset.id);
				if(lastDatasetId == null || datasetId != lastDatasetId) {
					lastDatasetId = datasetId;
					
					String uuid = tpsd.get(dataset.metadataIdentification);
					String fileIdentification = tpsd.get(dataset.metadataFileIdentification);
					String uuidref = config.getUrlPrefix() + "dataset/" + getName(fileIdentification);
					
					metadataDocument.addOperatesOn(uuid, uuidref);
				}
			}
			
			metadataDocument.removeServiceType();
			metadataDocument.removeServiceEndpoint();			
			metadataDocument.removeBrowseGraphic();
			metadataDocument.removeServiceLinkage();
			metadataDocument.removeSVCoupledResource();
			
			JsonNode serviceInfo = Json.parse(ts.get(publishedService.content));
			String serviceName = serviceInfo.get("name").asText();
			
			List<String> environmentIds = tx.query().from(publishedServiceEnvironment)
				.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
				.list(environment.identification);
			
			if(id.equals(ts.get(service.wmsMetadataFileIdentification))) {
				// WMS:				
				for(String environmentId : environmentIds) {
					String linkage = getServiceLinkage(environmentId, serviceName, ServiceType.WMS);
					
					String browseGraphicBaseUrl = linkage 
						+ "request=GetMap&Service=WMS&SRS=EPSG:28992&CRS=EPSG:28992"
						+ "&Bbox=180000,459000,270000,540000&Width=600&Height=662&Format=image/png&Styles=";
					
					metadataDocument.addServiceType("view");
					metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
					
					for(Tuple tpsd : ltpsd) {
						String identifier = tpsd.get(dataset.metadataFileIdentification);
						String layerName = tpsd.get(publishedServiceDataset.layerName);
						String scopedName = layerName;
						
						metadataDocument.addBrowseGraphic(browseGraphicBaseUrl + "&layers=" + layerName);
						metadataDocument.addServiceLinkage(linkage, ServiceType.WMS.getProtocol(), scopedName);
						metadataDocument.addSVCoupledResource("GetMap", identifier, scopedName);
					}
				}
			} else {
				// WFS:
				for(String environmentId : environmentIds) {
					String linkage = getServiceLinkage(environmentId, serviceName, ServiceType.WFS);
					
					metadataDocument.addServiceType("download");
					metadataDocument.addServiceEndpoint(ENDPOINT_OPERATION_NAME, ENDPOINT_CODE_LIST, ENDPOINT_CODE_LIST_VALUE, linkage);
					
					for(Tuple tpsd : ltpsd) {
						String identifier = tpsd.get(dataset.metadataFileIdentification);
						String layerName = tpsd.get(publishedServiceDataset.layerName);
						String scopedName = layerName;
						
						metadataDocument.addServiceLinkage(linkage, ServiceType.WFS.getProtocol(), scopedName);
						metadataDocument.addSVCoupledResource("GetFeature", identifier, scopedName);
					}
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
		return getId(name).flatMap(id -> Optional.empty());
	}
}
