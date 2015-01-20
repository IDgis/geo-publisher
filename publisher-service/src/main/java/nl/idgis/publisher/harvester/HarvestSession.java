package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.UpdateJobState;

import nl.idgis.publisher.dataset.messages.AlreadyRegistered;
import nl.idgis.publisher.dataset.messages.RegisterSourceDataset;
import nl.idgis.publisher.dataset.messages.Registered;
import nl.idgis.publisher.dataset.messages.Updated;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.harvest.HarvestLogType;
import nl.idgis.publisher.domain.job.harvest.HarvestLog;
import nl.idgis.publisher.domain.service.Dataset;

import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

public class HarvestSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, datasetManager;
	
	private final HarvestJobInfo harvestJob;
	
	private FutureUtils f;
	
	public HarvestSession(ActorRef database, ActorRef datasetManager, HarvestJobInfo harvestJob) {
		this.database = database;
		this.datasetManager = datasetManager;
		this.harvestJob = harvestJob;
	}
	
	public static Props props(ActorRef database, ActorRef datasetManager, HarvestJobInfo harvestJob) {
		return Props.create(HarvestSession.class, database, datasetManager, harvestJob);
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.apply(30, TimeUnit.SECONDS));
		
		f = new FutureUtils(getContext().dispatcher());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			handleTimeout();
		} else if(msg instanceof Dataset) {
			handleDataset((Dataset)msg);			
		} else if(msg instanceof End) {
			handleEnd();
		} else if(msg instanceof Log) {
			handleJobLog((Log)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleTimeout() {
		log.error("timeout while executing job: " + harvestJob);
		
		f.ask(database, new UpdateJobState(harvestJob, JobState.FAILED)).thenRun(() -> {
			log.debug("harvesting of dataSource finished: " + harvestJob);
			getContext().stop(getSelf());
		});
	}

	private void handleJobLog(Log msg) { 
		log.debug("saving job log");
		
		database.tell(new StoreLog(harvestJob, msg), getSender());
	}

	private void handleEnd() {
		log.debug("harvesting finished");
		
		f.ask(database, new UpdateJobState(harvestJob, JobState.SUCCEEDED)).thenRun(() -> {
			log.debug("harvesting of dataSource finished: " + harvestJob);
			getContext().stop(getSelf());
		});
	}

	private void handleDataset(final Dataset dataset) {
		log.debug("dataset received");
		
		ActorRef sender = getSender();
		String dataSourceId = harvestJob.getDataSourceId();		
		f.ask(datasetManager, new RegisterSourceDataset(dataSourceId, dataset)).thenAccept((msg) -> {			
			if(msg instanceof AlreadyRegistered) {
				log.debug("already registered");
				
				sender.tell(new NextItem(), getSelf());
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
									null
						));
					
					f.ask(database, new StoreLog(harvestJob, jobLog)).thenRun(() -> {
						log.debug("dataset registration logged");
						
						sender.tell(new NextItem(), getSelf());
					});
				} else {
					log.error("unknown dataset registration result: "+ msg);
					
					sender.tell(new NextItem(), getSelf());
				}
			}
		});
	}
}
