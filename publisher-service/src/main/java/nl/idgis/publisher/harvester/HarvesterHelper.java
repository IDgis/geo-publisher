package nl.idgis.publisher.harvester;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.domain.web.Metadata;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.utils.FutureUtils;

public class HarvesterHelper {

	private final ActorRef harvester;
	
	private final FutureUtils f;
	
	private final LoggingAdapter log;
	
	public HarvesterHelper(ActorRef harvester, FutureUtils f, LoggingAdapter log) {
		Objects.requireNonNull(harvester);
		Objects.requireNonNull(f);
		Objects.requireNonNull(log);
		
		this.harvester = harvester;
		this.f = f;
		this.log = log;
	}
	
	public CompletableFuture<Optional<Metadata>> getMetadata(String dataSourceId, String datasetId, Optional<String> stylesheet) {
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
	
	public CompletableFuture<Optional<Metadata>> getMetadata(ActorRef dataSource, String datasetId, Optional<String> stylesheet) {
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
