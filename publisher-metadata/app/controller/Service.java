package controller;

import javax.inject.Inject;

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
import play.mvc.Controller;
import play.mvc.Result;

import router.dav.SimpleWebDAV;

import util.QueryDSL;

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
	
	private final QueryDSL q;
	
	private final MetadataDocumentFactory mdf;
	
	@Inject
	public Service(QueryDSL q) throws Exception {
		this(q, new MetadataDocumentFactory(), "/");
	}
	
	public Service(QueryDSL q, MetadataDocumentFactory mdf, String prefix) {
		super(prefix);
		
		this.q = q;
		this.mdf = mdf;
	}
	
	@Override
	public Router withPrefix(String prefix) {
		return new Service(q, mdf, prefix);
	}
	
	private Optional<String> getId(String name) {
		if(name.toLowerCase().endsWith(".xml")) {
			return Optional.of(name.substring(0, name.length() - 4));
		} else {
			return Optional.empty();
		}
	}

	public Optional<Resource> resource(String name) {
		return getId(name).flatMap(id -> Optional.empty());
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
						.where(new SQLSubQuery().from(publishedServiceEnvironment)
							.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
							.where(publishedServiceEnvironment.serviceId.eq(service.id))
							.where(environment.confidential.isFalse())
							.exists())
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
