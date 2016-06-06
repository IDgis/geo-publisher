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
import play.Logger;
import play.api.mvc.Handler;
import play.api.mvc.RequestHeader;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import util.MetadataConfig;
import util.QueryDSL;
import util.QueryDSL.Transaction;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetMetadataAttachment.sourceDatasetMetadataAttachment;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QEnvironment.environment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class DatasetMetadata extends AbstractMetadata {
		
	private final MetadataDocumentFactory mdf;
	
	private final Pattern urlPattern;
	
	@Inject
	public DatasetMetadata(WebJarAssets webJarAssets, MetadataConfig config, QueryDSL q) throws Exception {
		this(webJarAssets, config, q, new MetadataDocumentFactory(), "/");
	}
	
	public DatasetMetadata(WebJarAssets webJarAssets, MetadataConfig config, QueryDSL q, MetadataDocumentFactory mdf, String prefix) {
		super(webJarAssets, config, q, prefix);
		
		this.mdf = mdf;
		urlPattern = Pattern.compile(".*/(.*)(\\?.*)?$");
	}
	
	@Override
	public DatasetMetadata withPrefix(String prefix) {
		return new DatasetMetadata(webJarAssets, config, q, mdf, prefix);
	}
	
	private String stylesheet() {
		if(isTrusted()) {
			return "datasets/intern/metadata.xsl";
		} else {
			return "datasets/extern/metadata.xsl";
		}
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
						sourceDatasetMetadata.sourceDatasetId,
						sourceDatasetMetadata.document);
				
				if(datasetTuple == null) {
					return Optional.<Resource>empty();
				}
				
				int sourceDatasetId = datasetTuple.get(sourceDatasetMetadata.sourceDatasetId);
				int datasetId = datasetTuple.get(dataset.id);
				
				Map<String, Integer> attachments = tx.query().from(sourceDatasetMetadataAttachment.sourceDatasetMetadataAttachment)
					.where(sourceDatasetMetadataAttachment.sourceDatasetId.eq(sourceDatasetId))
					.list(
						sourceDatasetMetadataAttachment.id,
						sourceDatasetMetadataAttachment.identification)
					.stream()
					.collect(Collectors.toMap(
						t -> t.get(sourceDatasetMetadataAttachment.identification),
						t -> t.get(sourceDatasetMetadataAttachment.id)));
				
				List<Tuple> serviceTuples = tx.query().from(publishedService)
					.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
					.join(environment).on(environment.id.eq(publishedService.environmentId))
					.where(publishedServiceDataset.datasetId.eq(datasetId))
					.list(
						publishedService.content,
						environment.identification,
						environment.url,
						publishedServiceDataset.layerName);
				
				MetadataDocument metadataDocument = mdf.parseDocument(datasetTuple.get(sourceDatasetMetadata.document));
				
				metadataDocument.setDatasetIdentifier(datasetTuple.get(dataset.metadataIdentification));
				metadataDocument.setFileIdentifier(id);
				
				for(String supplementalInformation : metadataDocument.getSupplementalInformation()) {
					int separator = supplementalInformation.indexOf("|");
					if(separator != -1 && attachments.containsKey(supplementalInformation)) {
						String type = supplementalInformation.substring(0, separator);
						String url = supplementalInformation.substring(separator + 1).trim().replace('\\', '/');
						
						String fileName;
						Matcher urlMatcher = urlPattern.matcher(url);
						if(urlMatcher.find()) {
							fileName = urlMatcher.group(1);
						} else {
							fileName = "download";
						}
						
						String updatedSupplementalInformation = 
							type + "|" + 
								routes.Attachment.get(attachments.get(supplementalInformation).toString(), fileName)
								.absoluteURL(false, config.getHost());
						
						metadataDocument.updateSupplementalInformation(
							supplementalInformation,
							updatedSupplementalInformation);
					}
				}
				
				List<String> browseGraphics = metadataDocument.getDatasetBrowseGraphics();
				for(String browseGraphic : browseGraphics) {
					if(attachments.containsKey(browseGraphic)) {
						String url = browseGraphic.trim().replace('\\', '/');
						
						String fileName;
						Matcher urlMatcher = urlPattern.matcher(url);
						if(urlMatcher.find()) {
							fileName = urlMatcher.group(1);
						} else {
							fileName = "preview";
						}
						
						String updatedbrowseGraphic =
							routes.Attachment.get(attachments.get(browseGraphic).toString(), fileName)
							.absoluteURL(false, config.getHost());
						
						metadataDocument.updateDatasetBrowseGraphic(browseGraphic, updatedbrowseGraphic);
					}
				}
				
				metadataDocument.removeServiceLinkage();
				for(Tuple serviceTuple : serviceTuples) {
					JsonNode serviceInfo = Json.parse(serviceTuple.get(publishedService.content));
					
					String serviceName = serviceInfo.get("name").asText();
					String environmentId = serviceTuple.get(environment.identification);
					String scopedName = serviceTuple.get(publishedServiceDataset.layerName);
					String environmentUrl = serviceTuple.get(environment.url);
					
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
						String linkage = getServiceLinkage(environmentUrl, serviceName, ServiceType.WMS);
						metadataDocument.addDatasetBrowseGraphic(linkage + BROWSE_GRAPHIC_WMS_REQUEST + scopedName);
					}
					
					for(ServiceType serviceType : ServiceType.values()) {
						String linkage = getServiceLinkage(environmentUrl, serviceName, serviceType);
						String protocol = serviceType.getProtocol();
						
						metadataDocument.addServiceLinkage(linkage, protocol, scopedName);
					}
				}
				
				metadataDocument.setStylesheet(routes.WebJarAssets.at(webJarAssets.locate(stylesheet())).url());
				
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
