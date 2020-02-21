package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QDataSource.dataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.messages.DataSourceInfo;
import nl.idgis.publisher.database.messages.QDataSourceInfo;

import nl.idgis.publisher.domain.query.HarvestDatasources;
import nl.idgis.publisher.domain.query.PerformPublish;
import nl.idgis.publisher.domain.query.RefreshDataset;
import nl.idgis.publisher.domain.web.Dataset;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.Style;

import nl.idgis.publisher.harvester.messages.GetActiveDataSources;
import nl.idgis.publisher.job.manager.messages.CreateHarvestJob;
import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.GetServicesWithDataset;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.service.manager.messages.GetServicesWithStyle;
import nl.idgis.publisher.utils.TypedIterable;

import akka.actor.ActorRef;
import akka.actor.Props;

public class JobCreator extends AbstractAdmin {
	
	private final ActorRef serviceManager, jobManager, harvester;

	public JobCreator(ActorRef database, ActorRef serviceManager, ActorRef jobManager, final ActorRef harvester) {
		super(database);
		
		this.serviceManager = serviceManager;
		this.jobManager = jobManager;
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, ActorRef jobManager, final ActorRef harvester) {
		return Props.create(JobCreator.class, database, serviceManager, jobManager, harvester);
	}

	private CompletableFuture<Boolean> createImportJob(String datasetId) {
		log.debug("requesting to refresh dataset: {}", datasetId);
		
		return f.ask(jobManager, new CreateImportJob(datasetId), Ack.class) .thenApply(msg -> true);
	}

	@Override
	protected void preStartAdmin() {
		doQuery(RefreshDataset.class, refreshDataset -> createImportJob(refreshDataset.getDatasetId()));
		doQuery(HarvestDatasources.class, this::handleHarvestDatasources);
	}
	
	@SuppressWarnings("unchecked")
	private CompletableFuture<Set<String>> activeDataSources() {
		return f.ask(harvester, new GetActiveDataSources(), Set.class).thenApply(resp -> resp);
	}
	
	private CompletableFuture<Boolean> handleHarvestDatasources (final HarvestDatasources message) {
		return
				db.query().from(dataSource)
				.orderBy(dataSource.identification.asc())
				.list(new QDataSourceInfo(dataSource.identification, dataSource.name))
				.thenCompose(dataSourceInfos -> 
					activeDataSources().thenCompose(activeDataSources -> {
						final List<CompletableFuture<Ack>> futures = new ArrayList<> (1);
						
						for (final DataSourceInfo dataSourceInfo: dataSourceInfos) {
							if (!activeDataSources.contains (dataSourceInfo.getId ())) {
								continue;
							}
							
							if (message.getDatasourceId () != null && !dataSourceInfo.getId ().equals (message.getDatasourceId ())) {
								continue;
							}

							futures.add (f.ask (jobManager, new CreateHarvestJob (dataSourceInfo.getId ()), Ack.class));
						}

						return CompletableFuture.allOf (futures.toArray (new CompletableFuture[futures.size ()])).thenApply ((a) -> true);
					}));
	}
}
