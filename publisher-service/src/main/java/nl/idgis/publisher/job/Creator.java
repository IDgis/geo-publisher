package nl.idgis.publisher.job;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.database.messages.CreateHarvestJob;
import nl.idgis.publisher.database.messages.CreateImportJob;
import nl.idgis.publisher.database.messages.CreateServiceJob;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDataSourceStatus;
import nl.idgis.publisher.database.messages.DataSourceStatus;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.utils.TypedIterable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Creator extends Scheduled {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	
	//private static final FiniteDuration HARVEST_INTERVAL = Duration.create(15, TimeUnit.MINUTES);
	private static final FiniteDuration HARVEST_INTERVAL = Duration.create(1, TimeUnit.MINUTES);
	
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
	
	private void scheduleImportJobs(Iterable<DatasetStatusInfo> datasetStatuses) {
		log.debug("scheduling import jobs");
		
		for(DatasetStatusInfo datasetStatus : datasetStatuses) {
			String datasetId = datasetStatus.getDatasetId();
			
			boolean needsImport = false;
			
			if(!datasetStatus.isImported()) {
				needsImport = true;				
				
				log.debug("not yet imported -> needs import");
			} else {
								
				if(datasetStatus.isSourceDatasetRevisionChanged()) {
					needsImport = true;				
					
					log.debug("revision changed -> needs import");									
				} else {
					log.debug("revision unchanged");
				}
											
				if(datasetStatus.isSourceDatasetColumnsChanged()) {
					needsImport = true;
					
					log.debug("sourceColumns changed -> needs import");
				} else {
					log.debug("sourceColumns unchanged");					
				}
				
				if(datasetStatus.isColumnsChanged()) {
					needsImport = true;
					
					log.debug("columns changed -> needs import");					
				} else {
					log.debug("columns unchanged");				
				}				
				
				if(datasetStatus.isSourceDatasetChanged()) {
					needsImport = true;
					
					log.debug("source dataset changed -> needs import");					
				} else {
					log.debug("source dataset unchanged");
				}
				
				if(datasetStatus.isFilterConditionChanged()) {
					needsImport = true;
					
					log.debug("filter condition changed -> needs import");
				} else {
					log.debug("filter condition unchanged");
				}
			}
			
			if(needsImport) {
				log.debug("creating import job for: " + datasetId);
				
				database.tell(new CreateImportJob(datasetId), getSelf());	
			} else {
				log.debug("no imported need for: " + datasetId);				
			}
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
		} else if(msg.contains(DatasetStatusInfo.class)) {
			Iterable<DatasetStatusInfo> datasetStatuses = msg.cast(DatasetStatusInfo.class);
			
			scheduleImportJobs(datasetStatuses);
			scheduleServiceJobs(datasetStatuses);
		} else {
			unhandled(msg);
		}
	}

	private void scheduleServiceJobs(Iterable<DatasetStatusInfo> datasetStatuses) {		
		for(DatasetStatusInfo datasetStatus : datasetStatuses) {
			String datasetId = datasetStatus.getDatasetId();
			
			if(datasetStatus.isServiceCreated()) {
				log.debug("service already created for dataset: " + datasetId);
			} else {
				log.debug("creating service for dataset: " + datasetId);
				
				database.tell(new CreateServiceJob(datasetId), getSelf());
			}
		}
	}

	@Override
	protected void doInitiate() {
		log.debug("requesting statuses");
		
		database.tell(new GetDataSourceStatus(), getSelf());
		database.tell(new GetDatasetStatus(), getSelf());
	}

}
