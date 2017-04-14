package controllers;

import javax.inject.Inject;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.JsonNode;
import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLQuery;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Expression;
import com.mysema.query.types.QTuple;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.NumberExpression;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;
import com.mysema.query.types.template.BooleanTemplate;

import nl.idgis.dav.model.Resource;
import nl.idgis.dav.model.ResourceDescription;
import nl.idgis.dav.model.ResourceProperties;

import nl.idgis.dav.model.DefaultResource;
import nl.idgis.dav.model.DefaultResourceDescription;
import nl.idgis.dav.model.DefaultResourceProperties;
import nl.idgis.publisher.database.QSourceDatasetVersion;
import nl.idgis.publisher.database.QSourceDatasetVersionColumn;
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
import static nl.idgis.publisher.database.QDatasetColumn.datasetColumn;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetMetadataAttachment.sourceDatasetMetadataAttachment;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersionColumn.sourceDatasetVersionColumn;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QDatasetCopy.datasetCopy;
import static nl.idgis.publisher.database.QDatasetView.datasetView;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class DatasetMetadata extends AbstractMetadata {
		
	private final MetadataDocumentFactory mdf;
	
	private final Pattern urlPattern;
	
	@Inject
	public DatasetMetadata(MetadataConfig config, QueryDSL q) throws Exception {
		this(config, q, new MetadataDocumentFactory(), "/");
	}
	
	public DatasetMetadata(MetadataConfig config, QueryDSL q, MetadataDocumentFactory mdf, String prefix) {
		super(config, q, prefix);
		
		this.mdf = mdf;
		urlPattern = Pattern.compile(".*/(.*)(\\?.*)?$");
	}
	
	@Override
	public DatasetMetadata withPrefix(String prefix) {
		return new DatasetMetadata(config, q, mdf, prefix);
	}
	
	private SQLQuery fromNonPublishedSourceDataset(Transaction tx) {
		return joinSourceDatasetVersion(
			tx.query().from(sourceDataset)
				.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
				.where(new SQLSubQuery().from(dataset)
					.where(dataset.sourceDatasetId.eq(sourceDataset.id))
					.where(isPublishedDataset())
					.notExists()));
	}
	
	private SQLQuery fromSourceDataset(Transaction tx) {
		return joinSourceDatasetVersion(
			tx.query().from(sourceDataset)
				.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id)));
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
	
	private SQLQuery fromPublishedDataset(Transaction tx) {
		return joinSourceDatasetVersion(tx.query().from(dataset)
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
			.where(isPublishedDataset()));
	}
	
	private SQLQuery fromDataset(Transaction tx) {
		return joinSourceDatasetVersion(tx.query().from(dataset)
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id)));
	}

	private BooleanExpression isPublishedDataset() {
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
					if(config.getIncludeSourceDatasetMetadata()) {
						return sourceDatasetResource(id, tx);
					} else {
						Optional<Resource> emptyOptional = Optional.empty();
						
						return emptyOptional;
					}
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
	
	private List<Tuple> datasetColumnAliases(Transaction tx, Expression<?> datasetRel, NumberPath<Integer> datasetRelId, StringPath datasetRelName, int datasetId) {
		final QSourceDatasetVersionColumn sourceDatasetVersionColumnSub = new QSourceDatasetVersionColumn("source_dataset_version_column_sub");
		final QSourceDatasetVersion sourceDatasetVersionSub = new QSourceDatasetVersion("source_dataset_version_sub");
		
		return tx.query().from(datasetRel)
			.join(dataset).on(dataset.id.eq(datasetRelId))
			.join(sourceDatasetVersionColumn).on(sourceDatasetVersionColumn.name.eq(datasetRelName))
			.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(sourceDatasetVersionColumn.sourceDatasetVersionId)
				.and(dataset.sourceDatasetId.eq(sourceDatasetVersion.sourceDatasetId)))
			.where(datasetRelId.eq(datasetId))
			.where(new SQLSubQuery().from(sourceDatasetVersionColumnSub)
				.join(sourceDatasetVersionSub).on(sourceDatasetVersionSub.id.eq(sourceDatasetVersionColumnSub.sourceDatasetVersionId))
				.where(sourceDatasetVersionColumnSub.name.eq(sourceDatasetVersionColumn.name))
				.where(sourceDatasetVersionColumnSub.sourceDatasetVersionId.gt(sourceDatasetVersionColumn.sourceDatasetVersionId))
				.where(sourceDatasetVersionSub.sourceDatasetId.eq(dataset.sourceDatasetId))
				.notExists())
			.where(sourceDatasetVersionColumn.alias.isNotNull())
			.orderBy(sourceDatasetVersionColumn.index.desc())
			.list(sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.alias);
	}

	private Resource tupleToDatasetResource(Transaction tx, Tuple datasetTuple, int sourceDatasetId, Integer datasetId, String fileIdentifier, String datasetIdentifier) {
		try {
			MetadataDocument metadataDocument = mdf.parseDocument(datasetTuple.get(sourceDatasetMetadata.document));
			
			metadataDocument.removeStylesheet();
			stylesheet("datasets").ifPresent(metadataDocument::setStylesheet);
			
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
				if(separator != -1) {
					String type = supplementalInformation.substring(0, separator);
					String url = supplementalInformation.substring(separator + 1).trim().replace('\\', '/');
					
					String fileName;
					Matcher urlMatcher = urlPattern.matcher(url);
					if(urlMatcher.find()) {
						fileName = urlMatcher.group(1);
					} else {
						fileName = "download";
					}
					
					if(attachments.containsKey(supplementalInformation)) {
						String updatedSupplementalInformation = 
							type + "|" + 
								routes.Attachment.get(attachments.get(supplementalInformation).toString(), fileName)
								.absoluteURL(false, config.getHost());
					
						metadataDocument.updateSupplementalInformation(
							supplementalInformation,
							updatedSupplementalInformation);
					} else {
						metadataDocument.removeSupplementalInformation(supplementalInformation);
					}
					
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
			
			Consumer<List<Tuple>> columnAliasWriter = columnTuples -> {
				if(columnTuples.isEmpty()) {
					return;
				}
				
				StringBuilder textAlias = new StringBuilder("INHOUD ATTRIBUTENTABEL:");
				
				for(Tuple columnTuple : columnTuples) {
					textAlias
						.append(" ")
						.append(columnTuple.get(sourceDatasetVersionColumn.name))
						.append(": ")
						.append(columnTuple.get(sourceDatasetVersionColumn.alias));
				}
				
				try {
					metadataDocument.addProcessStep(textAlias.toString());
				} catch(NotFound nf) {
					throw new RuntimeException(nf);
				}
			};
			
			if(datasetId == null) {
				columnAliasWriter.accept(
					tx.query().from(sourceDatasetVersionColumn)
						.where(sourceDatasetVersionColumn.sourceDatasetVersionId.eq(
							new SQLSubQuery().from(sourceDatasetVersion)
								.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDatasetId))
								.unique(sourceDatasetVersion.id.max())))
						.where(sourceDatasetVersionColumn.alias.isNotNull())
						.orderBy(sourceDatasetVersionColumn.index.desc())
						.list(sourceDatasetVersionColumn.name, sourceDatasetVersionColumn.alias));
			} else {
				final QSourceDatasetVersionColumn sourceDatasetVersionColumnSub = new QSourceDatasetVersionColumn("source_dataset_version_column_sub");
				
				List<Tuple> datasetCopyAliases = 
					datasetColumnAliases(
						tx, 
						datasetCopy, 
						datasetCopy.datasetId, 
						datasetCopy.name, 
						datasetId);
				
				if(datasetCopyAliases.isEmpty()) {
					columnAliasWriter.accept(
						datasetColumnAliases(
							tx, datasetView, 
							datasetView.datasetId, 
							datasetView.name, 
							datasetId));
				} else {
					columnAliasWriter.accept(datasetCopyAliases);
				}
				
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
						environment.confidential,
						environment.url,
						environment.wmsOnly,
						publishedServiceDataset.layerName);
				
				if(!serviceTuples.isEmpty()) {
					config.getDownloadUrlPrefix().ifPresent(downloadUrlPrefix -> {
						if(config.getDownloadUrlDisplay()) {
							try {
								metadataDocument.addServiceLinkage(downloadUrlPrefix + fileIdentifier, "download", null);
							} catch(NotFound nf) {
								throw new RuntimeException(nf);
							}
						}
					});
				}
				
				boolean confidential = true;
				boolean environmentWmsOnly = false;
				int serviceTupleIndex = 0;
				for(int i = 0; i < serviceTuples.size(); i++) {
					boolean envConfidential = serviceTuples.get(i).get(environment.confidential);
					environmentWmsOnly = serviceTuples.get(i).get(environment.wmsOnly);
					
					if(!envConfidential) {
						confidential = false;
						serviceTupleIndex = i;
						break;
					}
				}
				
				for(int i = 0; i < serviceTuples.size(); i++) {
					JsonNode serviceInfo = Json.parse(serviceTuples.get(i).get(publishedService.content));
					String serviceName = serviceInfo.get("name").asText();
					boolean wmsOnly = serviceInfo.get("wmsOnly").asBoolean();
					String scopedName = serviceTuples.get(i).get(publishedServiceDataset.layerName);
					
					if(i == serviceTupleIndex) {
						if(confidential) {
							config.getViewerUrlSecurePrefix().ifPresent(viewerUrlPrefix -> {
								try {
									metadataDocument.addServiceLinkage(viewerUrlPrefix + "/layer/" + serviceName + "/" + scopedName, "website", null);
								} catch(NotFound nf) {
									throw new RuntimeException(nf);
								}
							});
						} else if(environmentWmsOnly) {
							config.getViewerUrlWmsOnlyPrefix().ifPresent(viewerUrlPrefix -> {
								try {
									metadataDocument.addServiceLinkage(viewerUrlPrefix + "/layer/" + serviceName + "/" + scopedName, "website", null);
								} catch(NotFound nf) {
									throw new RuntimeException(nf);
								}
							});
						} else {
							config.getViewerUrlPublicPrefix().ifPresent(viewerUrlPrefix -> {
								try {
									metadataDocument.addServiceLinkage(viewerUrlPrefix + "/layer/" + serviceName + "/" + scopedName, "website", null);
								} catch(NotFound nf) {
									throw new RuntimeException(nf);
								}
							});
						}
					}
					
					String environmentUrl = serviceTuples.get(i).get(environment.url);
					
					// we only automatically generate browseGraphics 
					// when none where provided by the source. 
					if(browseGraphics.isEmpty()) {
						String linkage = getServiceLinkage(environmentUrl, serviceName, ServiceType.WMS);
						metadataDocument.addDatasetBrowseGraphic(linkage + config.getBrowseGraphicWmsRequest() + scopedName);
					}
					
					for(ServiceType serviceType : ServiceType.values()) {
						String linkage = getServiceLinkage(environmentUrl, serviceName, serviceType);
						String protocol = serviceType.getProtocol();
						
						for(String spatialSchema : metadataDocument.getSpatialSchema()) {
							if((!wmsOnly && spatialSchema.equals("vector")) || protocol.equals("OGC:WMS")) {
								metadataDocument.addServiceLinkage(linkage, protocol, scopedName);
							}
						}
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
		if(config.getIncludeSourceDatasetMetadata()) {
			return q.withTransaction(tx -> Stream.concat(
				datasetDescriptions(tx), 
				sourceDatasetDescriptions(tx)));
		} else {
			return q.withTransaction(this::datasetDescriptions);
		}
	}
	
	private Stream<ResourceDescription> sourceDatasetDescriptions(Transaction tx) {
		return fromNonPublishedSourceDataset(tx).list(
			sourceDataset.metadataFileIdentification,
			sourceDatasetVersion.metadataConfidential,
			sourceDatasetVersion.revision).stream()
			.map(tuple -> tupleToDatasetDescription(tuple, sourceDataset.metadataFileIdentification, false));
	}
	
	private Stream<ResourceDescription> datasetDescriptions(Transaction tx) {
		return fromPublishedDataset(tx).list(
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
					if(config.getIncludeSourceDatasetMetadata()) {
						return sourceDatasetProperties(id, tx);
					} else {
						Optional<ResourceProperties> emptyOptional = Optional.empty();
						
						return emptyOptional;
					}
				}
		}));
	}
	
	private Optional<ResourceProperties> sourceDatasetProperties(String id, Transaction tx) {
		return tupleToDatasetProperties(
			fromSourceDataset(tx).where(sourceDataset.metadataFileIdentification.eq(id)),
			BooleanTemplate.FALSE);
	}

	private Optional<ResourceProperties> datasetProperties(String id, Transaction tx) {
		return tupleToDatasetProperties(
			fromDataset(tx).where(dataset.metadataFileIdentification.eq(id)),
			isPublishedDataset());
	}
	
	private Optional<ResourceProperties> tupleToDatasetProperties(SQLQuery query, BooleanExpression isPublished) {
		final BooleanExpression isPublishedAliased = isPublished.as("is_published");
		
		return Optional.ofNullable(query
			.singleResult(sourceDatasetVersion.revision, sourceDatasetVersion.metadataConfidential, isPublishedAliased))
			.map(datasetTuple -> {
				Timestamp createTime = datasetTuple.get(sourceDatasetVersion.revision);
				boolean confidential = datasetTuple.get(sourceDatasetVersion.metadataConfidential);
				
				return new DefaultResourceProperties(
					false, 
					createTime,
					resourceProperties(confidential, datasetTuple.get(isPublishedAliased)));
			});
	}

	private Map<QName, String> resourceProperties(boolean confidential, boolean published) {
		Map<QName, String> properties = new HashMap<QName, String>();
		properties.put(new QName("http://idgis.nl/geopublisher", "confidential"), "" + confidential);
		properties.put(new QName("http://idgis.nl/geopublisher", "published"), "" + published);
		return properties;
	}
}
