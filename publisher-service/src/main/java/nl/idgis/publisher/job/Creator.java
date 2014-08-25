package nl.idgis.publisher.job;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.DatasetStatus;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.utils.TypedIterable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Creator extends Scheduled {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	private static final FiniteDuration HARVEST_INTERVAL = Duration.create(15, TimeUnit.MINUTES);
	
	public Creator(ActorRef database) {
		this.database = database;
	}
	
	public static Props props(ActorRef database) {
		return Props.create(Creator.class, database);
	}
	
	@Override
	protected void doElse(Object msg) {
		log.debug("message received: "+ msg);
		
		if(msg instanceof TypedIterable) {
			doIterable((TypedIterable<?>)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void scheduleImportJobs(Iterable<DatasetStatus> datasetStatuses) {
		log.debug("scheduling import jobs");
		
		for(DatasetStatus datasetStatus : datasetStatuses) {
			String datasetId = datasetStatus.getDatasetId();
			
			Timestamp sourceRevision = datasetStatus.getSourceRevision();
			Timestamp importedRevision = datasetStatus.getImportedRevision();
			
			if(importedRevision == null) {
				log.debug("not yet imported");
			} else {
				List<Column> columns = datasetStatus.getColumns();
				List<Column> importedColumns = datasetStatus.getSourceColumns();
				List<Column> sourceColumns = datasetStatus.getSourceColumns();
				
				if(sourceRevision.getTime() != importedRevision.getTime()) {
					log.debug("revision changed");
										
					if(sourceColumns.equals(importedColumns)) {
						log.debug("sourceColumns unchanged");
					} else {
						log.debug("sourceColumns changed -> needs confirmation");
						continue;
					}
				} else {
					// TODO: also check for filter changes
					
					if(!columns.equals(importedColumns)) {
						log.debug("columns changed");
					} else {					
						log.debug("no imported need for: " + datasetId);
						continue;
					}
				}
			}
			
			log.debug("creating import job for: " + datasetId);
			database.tell(new CreateImportJob(datasetId), getSelf());
		}
	}
	
	private void scheduleHarvestJobs(Iterable<DataSourceStatus> dataSourceStatuses) {
		log.debug("scheduling harvest jobs");
		
		for(DataSourceStatus dataSourceStatus : dataSourceStatuses) {
			final String dataSourceId = dataSourceStatus.getDataSourceId();
			final JobState state = dataSourceStatus.getFinishedState();
			final Timestamp time = dataSourceStatus.getLastHarvested();
			
			final long timeDiff;
			if(time != null) {
				timeDiff = System.currentTimeMillis() - time.getTime();
			} else {
				timeDiff = Long.MAX_VALUE;
			}
			
			if(!JobState.SUCCEEDED.equals(state)
				|| timeDiff > HARVEST_INTERVAL.toMillis()) {
				
				log.debug("creating harvest job for: " + dataSourceId);				
				database.tell(new CreateHarvestJob(dataSourceId), getSelf());				
			} else {
				log.debug("not yet creating harvest job for: " + dataSourceId);
			}
		}
	}

	private void doIterable(TypedIterable<?> msg) {
		if(msg.contains(DataSourceStatus.class)) {
			scheduleHarvestJobs(msg.cast(DataSourceStatus.class));
		} else if(msg.contains(DatasetStatus.class)) {
			scheduleImportJobs(msg.cast(DatasetStatus.class));
		} else {
			unhandled(msg);
		}
	}

	@Override
	protected void doInitiate() {
		log.debug("requesting statuses");
		
		database.tell(new GetDataSourceStatus(), getSelf());
		database.tell(new GetDatasetStatus(), getSelf());
	}

}
