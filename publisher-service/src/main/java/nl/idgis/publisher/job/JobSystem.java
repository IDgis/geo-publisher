package nl.idgis.publisher.job;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.job.creator.Creator;
import nl.idgis.publisher.job.creator.messages.CreateHarvestJobs;
import nl.idgis.publisher.job.creator.messages.CreateImportJobs;
import nl.idgis.publisher.job.creator.messages.CreateJobs;
import nl.idgis.publisher.job.manager.JobManager;
import nl.idgis.publisher.job.manager.messages.GetHarvestJobs;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.job.manager.messages.GetRemoveJobs;
import nl.idgis.publisher.job.manager.messages.GetServiceJobs;
import nl.idgis.publisher.job.manager.messages.JobManagerRequest;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.FiniteDuration;

public class JobSystem extends UntypedActor {
	
	private final static FiniteDuration CREATE_JOB_INTERVAL = FiniteDuration.create(10, TimeUnit.SECONDS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader, service;
	
	private ActorRef jobManager, jobCreator;
	
	private FutureUtils f;
	
	public JobSystem(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		return Props.create(JobSystem.class, database, harvester, loader, service);
	}
	
	@Override
	public void preStart() throws Exception {
		jobManager = getContext().actorOf(
				JobManager.props(database), "manager");
		
		getContext().actorOf(
				Initiator.props()
					.add(harvester, "harvester", new GetHarvestJobs())
					.add(loader, "import", new GetImportJobs())
					.add(loader, "remove", new GetRemoveJobs())
					.add(service, "service", new GetServiceJobs())
					.create(jobManager), 
				"initiator");
		
		jobCreator = getContext().actorOf(Creator.props(jobManager, database), "creator");		

		for(CreateJobs msg : Arrays.asList(
			
				new CreateHarvestJobs(), 
				new CreateImportJobs())) {
			
			getSelf().tell(msg, getSelf());
		}
		
		f = new FutureUtils(getContext().dispatcher());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof JobManagerRequest) {
			jobManager.forward(msg, getContext());
		} else if(msg instanceof CreateJobs) {
			f.ask(jobCreator, msg).whenComplete((resp, t) -> {
				if(t != null) {
					log.error("couldn't create jobs: {}, {}", t, msg);
				} else {				
					if(resp instanceof Failure) {
						log.error("failure during job creation: {}, {}", resp, msg);
					} else {
						log.debug("jobs created: {}", msg);
					}
				}
				
				getContext().system().scheduler().scheduleOnce(CREATE_JOB_INTERVAL, getSelf(), 
						msg, getContext().dispatcher(), getSelf());
			});
		} else {
			unhandled(msg);
		}
	}

}
