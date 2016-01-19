package controller;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Predicate;
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
import play.mvc.Controller;
import play.mvc.Result;

import router.dav.SimpleWebDAV;

import util.QueryDSL;

import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;

public class Service extends SimpleWebDAV {
	
	private static final Predicate notConfidential = 
		new SQLSubQuery().from(publishedServiceEnvironment)
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(publishedServiceEnvironment.serviceId.eq(service.id))
			.where(environment.confidential.isFalse())
			.exists();
	
	private final QueryDSL q;
	
	private final MetadataDocument template;
	
	@Inject
	public Service(QueryDSL q) throws Exception {
		this(q, getTemplate(), "/");
	}
	
	private static MetadataDocument getTemplate() throws Exception {
		MetadataDocumentFactory mdf = new MetadataDocumentFactory();
		
		return mdf.parseDocument(
			Service.class
				.getClassLoader()
				.getResourceAsStream("nl/idgis/publisher/metadata/service_metadata.xml"));
	}
	
	public Service(QueryDSL q, MetadataDocument template, String prefix) {
		super(prefix);
		
		this.q = q;
		this.template = template;
	}
	
	@Override
	public Router withPrefix(String prefix) {
		return new Service(q, template, prefix);
	}
	
	private Optional<String> getId(String name) {
		if(name.toLowerCase().endsWith(".xml")) {
			return Optional.of(name.substring(0, name.length() - 4));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id ->
			q.withTransaction(tx -> {
			
			Tuple t = tx.query().from(service)
				.join(constants).on(constants.id.eq(service.constantsId))
				.where(notConfidential)
				.where(service.wmsMetadataFileIdentification.eq(id)
					.or(service.wfsMetadataFileIdentification.eq(id)))
				.singleResult(
					service.wmsMetadataFileIdentification,
					service.wfsMetadataFileIdentification,
					service.alternateTitle);
			
			if(t == null) {
				return Optional.<Resource>empty();
			}
			
			MetadataDocument document = template.clone();
			document.setFileIdentifier(id);
			
			if(id.equals(t.get(service.wmsMetadataFileIdentification))) {
				
			} else {
				
			}
			
			return Optional.<Resource>of(new DefaultResource("application/xml", document.getContent()));
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
	public List<ResourceDescription> descriptions() {
		return q.withTransaction(tx -> {
			
			return 
				Stream.concat(
					Stream.of(new DefaultResourceDescription("", new DefaultResourceProperties(true))),
					tx.query().from(service)
						.where(notConfidential)
						.list(service.wmsMetadataFileIdentification, service.wfsMetadataFileIdentification).stream()
						.flatMap(t ->
							Stream.of(
								new ServiceInfo(t.get(service.wmsMetadataFileIdentification), t),
								new ServiceInfo(t.get(service.wfsMetadataFileIdentification), t)))
						.map(info -> {
							ResourceProperties properties = new DefaultResourceProperties(false);

							return new DefaultResourceDescription(info.id + ".xml", properties);
						}))
				.collect(Collectors.toList());
		});
	}

	@Override
	public Optional<ResourceProperties> properties(String name) {
		return getId(name).flatMap(id -> Optional.empty());
	}
}
