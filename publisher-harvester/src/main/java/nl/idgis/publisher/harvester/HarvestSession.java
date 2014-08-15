package nl.idgis.publisher.harvester;

import nl.idgis.publisher.database.messages.AlreadyRegistered;
import nl.idgis.publisher.database.messages.HarvestJob;
import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.UpdateJobState;

import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.HarvestJobLogType;
import nl.idgis.publisher.domain.job.harvest.SourceDatasetRegistration;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.harvester.sources.messages.Finished;
import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class HarvestSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final HarvestJob harvestJob;
	
	public HarvestSession(ActorRef database, HarvestJob harvestJob) {
		this.database = database;
		this.harvestJob = harvestJob;
	}
	
	public static Props props(ActorRef database, HarvestJob harvestJob) {
		return Props.create(HarvestSession.class, database, harvestJob);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Dataset) {
			handleDataset((Dataset)msg);			
		} else if(msg instanceof Finished) {
			handleFinished();
		} else if(msg instanceof JobLog) {
			handleJobLog((JobLog)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleJobLog(JobLog msg) { 
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
						
						JobLog jobLog = new JobLog(
								LogLevel.INFO, 
								HarvestJobLogType.SOURCE_DATASET_REGISTERED,
								new SourceDatasetRegistration(dataset.getId()));
						
						Patterns.ask(database, new StoreLog(harvestJob, jobLog), 15000)
							.onSuccess(new OnSuccess<Object>() {
								
								@Override
								public void onSuccess(Object msg) throws Throwable {
									log.debug("dataset registration logged");
									
									sender.tell(new Ack(), getSelf());
								}
							}, getContext().dispatcher());
					}
				}
			}, getContext().dispatcher());
	}
}
