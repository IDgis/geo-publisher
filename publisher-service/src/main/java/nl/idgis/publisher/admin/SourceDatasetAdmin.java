package nl.idgis.publisher.admin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.domain.query.GetMetadata;
import nl.idgis.publisher.domain.web.Metadata;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.MetadataDocument;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataSource.dataSource;

public class SourceDatasetAdmin extends AbstractAdmin {

	private final ActorRef harvester;
	
	public SourceDatasetAdmin(ActorRef database, ActorRef harvester) {
		super(database);	
		
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(SourceDatasetAdmin.class, database, harvester);
	}

	@Override
	protected void preStartAdmin() {
		doQueryOptional(GetMetadata.class, this::handleGetMetadata);
	}
	
	private CompletableFuture<Optional<Metadata>> handleGetMetadata(GetMetadata query) {
		String sourceDatasetId = query.id();
		log.debug("fetching metadata of source dataset: {}", sourceDatasetId);
		
		return db.query().from(sourceDataset)
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.where(sourceDataset.identification.eq(sourceDatasetId))
			.singleResult(
				dataSource.identification,
				sourceDataset.externalIdentification).thenCompose(optionalResult ->
					optionalResult
						.map(result -> getMetadata(
							result.get(dataSource.identification),
							result.get(sourceDataset.externalIdentification),
							Optional.ofNullable(query.stylesheet())))
						.orElseGet(() -> {
							log.warning("source dataset not found: {}", sourceDatasetId);
							
							return f.successful(Optional.empty());
						}));
	}
	
	private CompletableFuture<Optional<Metadata>> getMetadata(String dataSourceId, String datasetId, Optional<String> stylesheet) {
		return f.ask(harvester, new GetDataSource(dataSourceId)).<Optional<Metadata>>thenCompose(resp -> {
			if(resp instanceof NotConnected) {
				log.warning("data source not connected: {}" + dataSourceId);
				
				return f.successful(Optional.empty());
			} else if(resp instanceof ActorRef) {
				ActorRef dataSource = (ActorRef)resp;
				return getMetadata(dataSource, datasetId, stylesheet);
			} else {
				throw new IllegalStateException("ActorRef or NotConnected expected");
			}
		});
	}
	
	private CompletableFuture<Optional<Metadata>> getMetadata(ActorRef dataSource, String datasetId, Optional<String> stylesheet) {
		return f.ask(dataSource, new GetDatasetMetadata(datasetId), MetadataDocument.class).thenCompose(metadata -> {
			try {
				if(stylesheet.isPresent()) {
					metadata.setStylesheet(stylesheet.get());
				} else {
					metadata.removeStylesheet();
				}
				return f.successful(Optional.of(new Metadata(datasetId, metadata.getContent())));
			} catch(Exception e) {
				log.error("couldn't retrieve metadata: {}", e);
				
				return f.failed(e);
			}
		});
	}
}
