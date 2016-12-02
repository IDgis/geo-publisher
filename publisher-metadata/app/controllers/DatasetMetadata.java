package controllers;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Expression;
import com.mysema.query.types.QTuple;
import com.mysema.query.types.expr.BooleanExpression;

import nl.idgis.dav.model.Resource;
import nl.idgis.dav.model.ResourceDescription;
import nl.idgis.dav.model.ResourceProperties;

import nl.idgis.dav.model.DefaultResource;
import nl.idgis.dav.model.DefaultResourceDescription;
import nl.idgis.dav.model.DefaultResourceProperties;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.xml.exceptions.NotFound;
import nl.idgis.publisher.xml.exceptions.QueryFailure;
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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
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
	
	private SQLQuery fromSourceDataset(Transaction tx) {
		return joinSourceDatasetVersion(
			tx.query().from(sourceDataset)
				.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
				.where(new SQLSubQuery().from(dataset)
					.where(dataset.sourceDatasetId.eq(sourceDataset.id))
					.where(isPublished())
					.notExists()));
	}

	private SQLQuery joinSourceDatasetVersion(SQLQuery query) {
		query
			.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
			.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
				.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
				.list(sourceDatasetVersion.id.max())));
		
		if(isTrusted()) {
			return query;
		} else {
			return query.where(sourceDatasetVersion.metadataConfidential.isFalse());
		}
	}
	
	private SQLQuery fromDataset(Transaction tx) {
		return joinSourceDatasetVersion(tx.query().from(dataset)
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
			.where(isPublished()));
	}

	private BooleanExpression isPublished() {
		return new SQLSubQuery().from(publishedServiceDataset)
			.where(publishedServiceDataset.datasetId.eq(dataset.id))
			.exists();
	}
	
	@Override
	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				Optional<Resource> optionalDataset = datasetResource(id, tx);
				if(optionalDataset.isPresent()) {
					return optionalDataset;
				} else {
					return sourceDatasetResource(id, tx);
				}
		}));
	}
	
	private Optional<Resource> sourceDatasetResource(String id, Transaction tx) {
		return Optional.ofNullable(fromSourceDataset(tx)
			.where(sourceDataset.metadataFileIdentification.eq(id))
			.singleResult(
				sourceDataset.metadataIdentification,
				sourceDatasetMetadata.sourceDatasetId,
				sourceDatasetMetadata.document))
			.map(datasetTuple -> tupleToDatasetResource(
				tx, 
				datasetTuple, 
				datasetTuple.get(sourceDatasetMetadata.sourceDatasetId), 
				null,
				id, 
				datasetTuple.get(sourceDataset.metadataIdentification)));
	}

	private Optional<Resource> datasetResource(String id, Transaction tx) throws Exception {
		return Optional.ofNullable(fromDataset(tx)
			.where(dataset.metadataFileIdentification.eq(id))
			.singleResult(
				dataset.id,
				dataset.metadataIdentification,
				sourceDatasetMetadata.sourceDatasetId,
				sourceDatasetMetadata.document))
			.map(datasetTuple -> tupleToDatasetResource(
				tx, 
				datasetTuple, 
				datasetTuple.get(sourceDatasetMetadata.sourceDatasetId), 
				datasetTuple.get(dataset.id),
				id, 
				datasetTuple.get(dataset.metadataIdentification)));
	}

	private Resource tupleToDatasetResource(Transaction tx, Tuple datasetTuple, int sourceDatasetId, Integer datasetId, String fileIdentifier, String datasetIdentifier) {
		try {
			MetadataDocument metadataDocument = mdf.parseDocument(datasetTuple.get(sourceDatasetMetadata.document));
			metadataDocument.setStylesheet(routes.WebJarAssets.at(webJarAssets.locate(stylesheet())).url());
			metadataDocument.setDatasetIdentifier(datasetIdentifier);
			metadataDocument.setFileIdentifier(fileIdentifier);
			
			if(!isTrusted()) {
				metadataDocument.removeAdditionalPointOfContacts();
			}
			
			Map<String, Integer> attachments = tx.query().from(sourceDatasetMetadataAttachment)
				.where(sourceDatasetMetadataAttachment.sourceDatasetId.eq(sourceDatasetId))
				.list(
					sourceDatasetMetadataAttachment.id,
					sourceDatasetMetadataAttachment.identification)
				.stream()
				.collect(Collectors.toMap(
					t -> t.get(sourceDatasetMetadataAttachment.identification),
					t -> t.get(sourceDatasetMetadataAttachment.id)));
			
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
			if(datasetId != null) {
				SQLQuery serviceQuery = tx.query().from(publishedService)
						.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
						.join(environment).on(environment.id.eq(publishedService.environmentId));
					
				if(!isTrusted()) {
					// do not generate links to services with confidential content as these are inaccessible.
					
					serviceQuery.where(environment.confidential.isFalse());
				}
				
				List<Tuple> serviceTuples = serviceQuery.where(publishedServiceDataset.datasetId.eq(datasetId))
					.list(
						publishedService.content,
						environment.identification,
						environment.url,
						publishedServiceDataset.layerName);
				
				if(!serviceTuples.isEmpty()) {
					config.getDownloadUrlPrefix().ifPresent(downloadUrlPrefix -> {
						try {
							metadataDocument.addServiceLinkage(downloadUrlPrefix + fileIdentifier, "download", null);
						} catch(NotFound nf) {
							throw new RuntimeException(nf);
						}
					});
				}
				
				for(Tuple serviceTuple : serviceTuples) {
					JsonNode serviceInfo = Json.parse(serviceTuple.get(publishedService.content));
					
					String serviceName = serviceInfo.get("name").asText();
					String environmentId = serviceTuple.get(environment.identification);
					String scopedName = serviceTuple.get(publishedServiceDataset.layerName);
					String environmentUrl = serviceTuple.get(environment.url);
					
					// we only automatically generate browseGraphics 
					// when none where provided by the source. 
					if(browseGraphics.isEmpty()) {
						String linkage = getServiceLinkage(environmentUrl, serviceName, ServiceType.WMS);
						metadataDocument.addDatasetBrowseGraphic(linkage + config.getBrowseGraphicWmsRequest() + scopedName);
					}
					
					for(ServiceType serviceType : ServiceType.values()) {
						String linkage = getServiceLinkage(environmentUrl, serviceName, serviceType);
						String protocol = serviceType.getProtocol();
						
						metadataDocument.addServiceLinkage(linkage, protocol, scopedName);
					}
				}
			}
			
			return new DefaultResource("application/xml", metadataDocument.getContent());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Stream<ResourceDescription> descriptions() {
		return q.withTransaction(tx -> Stream.concat(
			datasetDescriptions(tx), 
			sourceDatasetDescriptions(tx)));
	}
	
	public Stream<ResourceDescription> sourceDatasetDescriptions(Transaction tx) {
		return fromSourceDataset(tx).list(
			sourceDataset.metadataFileIdentification,
			sourceDatasetVersion.metadataConfidential,
			sourceDatasetVersion.revision).stream()
			.map(tuple -> tupleToDatasetDescription(tuple, sourceDataset.metadataFileIdentification, false));
	}
	
	public Stream<ResourceDescription> datasetDescriptions(Transaction tx) {
		return fromDataset(tx).list(
			dataset.metadataFileIdentification, 
			sourceDatasetVersion.metadataConfidential,
			sourceDatasetVersion.revision).stream()
			.map(tuple -> tupleToDatasetDescription(tuple, dataset.metadataFileIdentification, true));
	}

	private ResourceDescription tupleToDatasetDescription(Tuple tuple, Expression<String> identificationExpression, boolean published) {
		Timestamp createTime = tuple.get(sourceDatasetVersion.revision);
		boolean confidential = tuple.get(sourceDatasetVersion.metadataConfidential);
		
		return new DefaultResourceDescription(
			getName(tuple.get(identificationExpression)),
			new DefaultResourceProperties(
				false,
				createTime,
				resourceProperties(confidential, published)));
	}

	@Override
	public Optional<ResourceProperties> properties(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
				Optional<ResourceProperties> optionalProperties = datasetProperties(id, tx);
				if(optionalProperties.isPresent()) {
					return optionalProperties;
				} else {
					return sourceDatasetProperties(id, tx);
				}
		}));
	}
	
	private Optional<ResourceProperties> sourceDatasetProperties(String id, Transaction tx) {
		return tupleToDatasetProperties(
			fromSourceDataset(tx).where(sourceDataset.metadataFileIdentification.eq(id)),
			false);
	}

	private Optional<ResourceProperties> datasetProperties(String id, Transaction tx) {
		return tupleToDatasetProperties(
			fromDataset(tx).where(dataset.metadataFileIdentification.eq(id)),
			true);
	}
	
	private Optional<ResourceProperties> tupleToDatasetProperties(SQLQuery query, boolean published) {
		return Optional.ofNullable(query
			.singleResult(sourceDatasetVersion.revision, sourceDatasetVersion.metadataConfidential))
			.map(datasetTuple -> {
				Timestamp createTime = datasetTuple.get(sourceDatasetVersion.revision);
				boolean confidential = datasetTuple.get(sourceDatasetVersion.metadataConfidential);
				
				return new DefaultResourceProperties(
					false, 
					createTime,
					resourceProperties(confidential, published));
			});
	}

	private Map<QName, String> resourceProperties(boolean confidential, boolean published) {
		Map<QName, String> properties = new HashMap<QName, String>();
		properties.put(new QName("http://idgis.nl/geopublisher", "confidential"), "" + confidential);
		properties.put(new QName("http://idgis.nl/geopublisher", "published"), "" + published);
		return properties;
	}
}
