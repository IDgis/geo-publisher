package nl.idgis.publisher.harvester;

import nl.idgis.publisher.AbstractSession;

import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.Registered;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.database.messages.Updated;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.HarvestLogType;
import nl.idgis.publisher.domain.job.harvest.HarvestLog;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.domain.web.EntityType;

import nl.idgis.publisher.harvester.sources.messages.Finished;
import nl.idgis.publisher.messages.Timeout;
import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class HarvestSession extends AbstractSession {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final HarvestJobInfo harvestJob;
	
	public HarvestSession(ActorRef database, HarvestJobInfo harvestJob) {
		this.database = database;
		this.harvestJob = harvestJob;
	}
	
	public static Props props(ActorRef database, HarvestJobInfo harvestJob) {
		return Props.create(HarvestSession.class, database, harvestJob);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		scheduleTimeout();
		if(msg instanceof Timeout) {
			handleTimeout();
		} else if(msg instanceof Dataset) {
			handleDataset((Dataset)msg);			
		} else if(msg instanceof Finished) {
			handleFinished();
		} else if(msg instanceof Log) {
			handleJobLog((Log)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleTimeout() {
		log.debug("timeout while executing job: " + harvestJob);
		
		getContext().stop(getSelf());
	}

	private void handleJobLog(Log msg) { 
		log.debug("saving job log");
		
		database.tell(new StoreLog(harvestJob, msg), getSender());
	}

	private void handleFinished() {
		log.debug("harvesting finished");
		
		final ActorRef self = getSelf();			
		Patterns.ask(database, new UpdateJobState(harvestJob, JobState.SUCCEEDED), 150000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("harvesting of dataSource finished: " + harvestJob);
					getContext().stop(self);
				}
			}, getContext().dispatcher());
	}

	private void handleDataset(final Dataset dataset) {
		log.debug("dataset received");
		
		String dataSourceId = harvestJob.getDataSourceId();
		final ActorRef sender = getSender();
		Patterns.ask(database, new RegisterSourceDataset(dataSourceId, dataset), 15000)
			.onSuccess(new OnSuccess<Object>() {
				
				@Override
				public void onSuccess(Object msg) throws Throwable {
					if(msg instanceof AlreadyRegistered) {
						log.debug("already registered");
						
						sender.tell(new Ack(), getSelf());
					} else {						
						log.debug("dataset registered");
						
						HarvestLogType type = null;
						if(msg instanceof Registered) {
							type = HarvestLogType.REGISTERED;
						} else if(msg instanceof Updated) {
							type = HarvestLogType.UPDATED;
						}
						
						if(type != null) {
							Log jobLog = Log.create (
									LogLevel.INFO, 
									type, 
									new HarvestLog (
											EntityType.SOURCE_DATASET, 
											dataset.getId (), 
											dataset.getTable().getName ()
								));
							
							Patterns.ask(database, new StoreLog(harvestJob, jobLog), 15000)
								.onSuccess(new OnSuccess<Object>() {
									
									@Override
									public void onSuccess(Object msg) throws Throwable {
										log.debug("dataset registration logged");
										
										sender.tell(new Ack(), getSelf());
									}
								}, getContext().dispatcher());
						} else {
							log.error("unknown dataset registration result: "+ msg);
							
							sender.tell(new Ack(), getSelf());
						}
					}
				}
			}, getContext().dispatcher());
	}
}
