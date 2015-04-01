package nl.idgis.publisher.harvester;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.dataset.messages.AlreadyRegistered;
import nl.idgis.publisher.dataset.messages.Cleanup;
import nl.idgis.publisher.dataset.messages.DeleteSourceDatasets;
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

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

public class HarvestSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef jobContext, datasetManager;
	
	private final HarvestJobInfo harvestJob;
	
	private final Set<String> datasetIds;
	
	private FutureUtils f;
	
	public HarvestSession(ActorRef jobContext, ActorRef datasetManager, HarvestJobInfo harvestJob, Set<String> datasetIds) {
		this.jobContext = jobContext;
		this.datasetManager = datasetManager;
		this.harvestJob = harvestJob;
		this.datasetIds = datasetIds;
	}
	
	public static Props props(ActorRef jobContext, ActorRef datasetManager, HarvestJobInfo harvestJob, Set<String> datasetIds) {
		return Props.create(HarvestSession.class, jobContext, datasetManager, harvestJob, datasetIds);
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.apply(30, TimeUnit.SECONDS));
		
		f = new FutureUtils(getContext());
		
		log.debug("existing datasets: {}", datasetIds.size());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			handleTimeout();
		} else if(msg instanceof Dataset) {
			handleDataset((Dataset)msg);			
		} else if(msg instanceof End) {
			handleEnd();
		} else {
			unhandled(msg);
		}
	}

	private void handleTimeout() {
		log.error("timeout while executing job: " + harvestJob);
		
		f.ask(jobContext, new UpdateJobState(JobState.FAILED)).whenComplete((msg, t) -> {
			if(t != null) {
				log.error("couldn't change job state: {}", t);
			} else {			
				log.debug("harvesting of dataSource finished: " + harvestJob);				
			}
			
			getContext().stop(getSelf());
		});
	}	

	private void handleEnd() {
		log.debug("harvesting finished");
		
		if(datasetIds.isEmpty()) {
			log.debug ("no obsolete datasets");
			cleanup();
		} else {
			log.debug ("obsolete datasets: {}", datasetIds.size());
			
			f.ask (datasetManager, new DeleteSourceDatasets(
				harvestJob.getDataSourceId(), 
				datasetIds)).whenComplete ((message, error) -> {
					if (error != null) {
						log.error ("couldn't delete source datasets: {}", error);
						finish(JobState.FAILED);
					} else {
						cleanup();
					}
				});
		}
	}

	private void cleanup() {
		log.debug("cleaning up");
		
		f.ask (datasetManager, new Cleanup ()).whenComplete ((message, error) -> {			
			if (error != null) {
				log.error ("couldn't perform cleanup: {}", error);
				finish(JobState.FAILED);
			} else {
				log.debug ("obsolete records removed: {}", message);
				finish(JobState.SUCCEEDED);
			}
		});
	}

	private void finish(final JobState finishState) {
		log.debug("finishing harvest session: {}", finishState);
		
		ActorRef self = getSelf();
		f.ask(jobContext, new UpdateJobState(finishState)).whenComplete((msg, t) -> {
			if(t != null) {
				log.error("couldn't change job state: {}", t);
			} else {			
				log.debug("harvesting of dataSource finished: " + harvestJob);
			}
			
			self.tell(PoisonPill.getInstance(), self);
		});
	}

	private void handleDataset(final Dataset dataset) {
		log.debug("dataset received");
		
		ActorRef sender = getSender();
		
		String dataSourceId = harvestJob.getDataSourceId();
		datasetIds.remove(dataset.getId());
		
		f.ask(datasetManager, new RegisterSourceDataset(dataSourceId, dataset))
			.exceptionally(e -> new Failure(e)).thenAccept((msg) -> {			
			if(msg instanceof AlreadyRegistered) {
				log.debug("already registered");
				
				sender.tell(new NextItem(), getSelf());
			} else {
				HarvestLogType type = null;
				if(msg instanceof Registered) {
					log.debug("dataset registered");
					
					type = HarvestLogType.REGISTERED;
				} else if(msg instanceof Updated) {
					log.debug("dataset updated");
					
					type = HarvestLogType.UPDATED;
				}
				
				if(type != null) {
					Log jobLog = Log.create (
							LogLevel.INFO, 
							type, 
							new HarvestLog (
									EntityType.SOURCE_DATASET, 
									dataset.getId (), 
									dataset.getName()
						));
					
					f.ask(jobContext, jobLog).whenComplete((ack, t) -> {
						if(t != null) {
							log.error("couldn't store log: {}", t);
						} else {
							log.debug("dataset registration logged");
						}
						sender.tell(new NextItem(), getSelf());
					});
				} else if(msg instanceof Failure) {
					log.error("dataset registration failed: {}", msg);
					
					sender.tell(new NextItem(), getSelf());
				} else {
					log.error("unknown dataset registration result: {}", msg);
					
					sender.tell(new NextItem(), getSelf());
				}
			}
		});
	}
}
