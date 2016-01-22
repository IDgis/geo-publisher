package nl.idgis.publisher.admin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.types.QTuple;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.domain.query.GetMetadata;
import nl.idgis.publisher.domain.web.Metadata;

import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;

import static nl.idgis.publisher.database.QSourceDatasetMetadata.sourceDatasetMetadata;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;

public class SourceDatasetAdmin extends AbstractAdmin {
	
	private final MetadataDocumentFactory mdf;
			
	public SourceDatasetAdmin(ActorRef database) throws Exception {
		super(database);
		
		mdf = new MetadataDocumentFactory();
	}
	
	public static Props props(ActorRef database) {
		return Props.create(SourceDatasetAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doQueryOptional(GetMetadata.class, this::handleGetMetadata);
	}
	
	private CompletableFuture<Optional<Metadata>> handleGetMetadata(GetMetadata query) {
		String sourceDatasetId = query.id();
		log.debug("fetching metadata of source dataset: {}", sourceDatasetId);
		
		return db.query().from(sourceDataset)
			.join(sourceDatasetMetadata).on(sourceDatasetMetadata.sourceDatasetId.eq(sourceDataset.id))
			.where(sourceDataset.identification.eq(sourceDatasetId))
			.singleResult(new QTuple(sourceDatasetMetadata.document)).thenApply(optionalResult -> 
				optionalResult.flatMap(result ->
					Optional.ofNullable(result.get(sourceDatasetMetadata.document))
						.map(content -> new Metadata(query.id(), content))));
	}
}
