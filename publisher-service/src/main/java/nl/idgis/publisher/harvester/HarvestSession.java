package nl.idgis.publisher.harvester;

import java.util.concurrent.TimeUnit;

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

import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.HarvestJobInfo;
import nl.idgis.publisher.protocol.messages.Failure;
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
	
	private final ActorRef jobContext, datasetManager;
	
	private final HarvestJobInfo harvestJob;
	
	private FutureUtils f;
	
	public HarvestSession(ActorRef jobContext, ActorRef datasetManager, HarvestJobInfo harvestJob) {
		this.jobContext = jobContext;
		this.datasetManager = datasetManager;
		this.harvestJob = harvestJob;
	}
	
	public static Props props(ActorRef jobContext, ActorRef datasetManager, HarvestJobInfo harvestJob) {
		return Props.create(HarvestSession.class, jobContext, datasetManager, harvestJob);
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
		
		f.ask(jobContext, new UpdateJobState(JobState.SUCCEEDED)).whenComplete((msg, t) -> {
			if(t != null) {
				log.error("couldn't change job state: {}", t);
			} else {			
				log.debug("harvesting of dataSource finished: " + harvestJob);
			}
			
			getContext().stop(getSelf());
		});
	}

	private void handleDataset(final Dataset dataset) {
		log.debug("dataset received");
		
		ActorRef sender = getSender();
		String dataSourceId = harvestJob.getDataSourceId();
		
		f.ask(datasetManager, new RegisterSourceDataset(dataSourceId, dataset))
			.exceptionally(e -> new Failure(e)).thenAccept((msg) -> {			
			if(msg instanceof AlreadyRegistered) {
				log.debug("already registered");
				
				sender.tell(new NextItem(), getSelf());
			} else {
				HarvestLogType type = null;
				if(msg instanceof Registered) {
					type = HarvestLogType.REGISTERED;
				} else if(msg instanceof Updated) {
					type = HarvestLogType.UPDATED;
				}
				
				if(type != null) {
					log.debug("dataset registered");
					
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
