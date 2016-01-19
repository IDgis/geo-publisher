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

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;

public class Dataset extends SimpleWebDAV {
	
	private final QueryDSL q;
	
	private final MetadataDocumentFactory mdf;
	
	@Inject
	public Dataset(QueryDSL q) throws Exception {
		this(q, new MetadataDocumentFactory(), "/");
	}
	
	public Dataset(QueryDSL q, MetadataDocumentFactory mdf, String prefix) {
		super(prefix);
		
		this.q = q;
		this.mdf = mdf;
	}

	public Optional<Resource> resource(String name) {
		if(name.toLowerCase().endsWith(".xml")) {
			String id = name.substring(0, name.length() - 4);
			
			return q.withTransaction(tx -> {
				Tuple t = tx.query().from(dataset)
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
					.singleResult(new QTuple(sourceDatasetMetadata.document));
				
				if(t == null) {
					return Optional.<Resource>empty();
				}
				
				MetadataDocument document = mdf.parseDocument(t.get(sourceDatasetMetadata.document));
				document.removeStylesheet();
				
				return Optional.<Resource>of(new DefaultResource("application/xml", document.getContent()));
			});
		}
		
		return Optional.empty();
	}
	
	@Override
	public Router withPrefix(String prefix) {
		return new Dataset(q, mdf, prefix);
	}

	@Override
	public List<ResourceDescription> descriptions() {
		return q.withTransaction(tx -> {
			return tx.query().from(dataset)
				.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
				.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
				.join(sourceDatasetVersion).on(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
				.where(sourceDatasetVersion.id.in(new SQLSubQuery().from(sourceDatasetVersion)
					.where(sourceDatasetVersion.sourceDatasetId.eq(sourceDataset.id))
					.list(sourceDatasetVersion.id.max())))
				.where(sourceDatasetVersion.confidential.isFalse())
				.list(dataset.metadataFileIdentification).stream()
					.map(id -> {
						ResourceProperties properties = new DefaultResourceProperties();
						
						return new DefaultResourceDescription(id + ".xml", properties); 
					})
					.collect(Collectors.toList());
		});
	}

	@Override
	public Optional<ResourceProperties> properties(String name) {
		if(name.toLowerCase().endsWith(".xml")) {
			String id = name.substring(0, name.length() - 4);
			
			return q.withTransaction(tx -> {
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
						
					return Optional.<ResourceProperties>of(new DefaultResourceProperties());
				} else {
					return Optional.<ResourceProperties>empty();
				}
			});
		} else {
			return Optional.empty();
		}
	}
}
