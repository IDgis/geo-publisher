package nl.idgis.publisher.admin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.domain.query.GetMetadata;
import nl.idgis.publisher.domain.web.Metadata;

import nl.idgis.publisher.harvester.HarvesterHelper;

import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataSource.dataSource;

public class SourceDatasetAdmin extends AbstractAdmin {
	
	private final ActorRef harvester;
	
	private HarvesterHelper hh;
	
	public SourceDatasetAdmin(ActorRef database, ActorRef harvester) {
		super(database);
		
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(SourceDatasetAdmin.class, database, harvester);
	}

	@Override
	protected void preStartAdmin() {
		hh = new HarvesterHelper(harvester, f, log);
		
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
						.map(result -> hh.getMetadata(
							result.get(dataSource.identification),
							result.get(sourceDataset.externalIdentification),
							Optional.ofNullable(query.stylesheet())))
						.orElseGet(() -> {
							log.warning("source dataset not found: {}", sourceDatasetId);
							
							return f.successful(Optional.empty());
						}));
	}
	
	
}
