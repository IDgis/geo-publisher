package nl.idgis.publisher.job;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
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

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class JobSystem extends UntypedActor {	
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader, service;
	
	private ActorRef jobManager, jobCreator;
	
	private FutureUtils f;
	
	private Map<CreateJobs, FiniteDuration> createJobsIntervals;
	
	private static class ScheduleCreateJobs implements Serializable {
		
		private static final long serialVersionUID = 2468417795794834230L;
		
		private final CreateJobs createJobs;

		ScheduleCreateJobs(CreateJobs createJobs) {
			this.createJobs = createJobs;
		}
		
		public CreateJobs getCreateJobs() {
			return createJobs;
		}

		@Override
		public String toString() {
			return "ScheduleCreateJobs [createJobs=" + createJobs + "]";
		}
	}
	
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
		
		createJobsIntervals = new HashMap<>();
		createJobsIntervals.put(new CreateHarvestJobs(), Duration.apply(15, TimeUnit.MINUTES));
		createJobsIntervals.put(new CreateImportJobs(), Duration.apply(10, TimeUnit.SECONDS));
		
		createJobsIntervals.keySet().stream()
			.forEach(msg -> getSelf().tell(msg, getSelf()));
		
		f = new FutureUtils(getContext().dispatcher());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof JobManagerRequest) {
			jobManager.forward(msg, getContext());
		} else if(msg instanceof CreateJobs) {
			ActorRef self = getSelf();
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
								
				self.tell(new ScheduleCreateJobs((CreateJobs)msg), self);
			});
		} else if(msg instanceof ScheduleCreateJobs) {
			CreateJobs createJobs = ((ScheduleCreateJobs)msg).getCreateJobs();
			FiniteDuration interval = createJobsIntervals.get(createJobs);
			
			log.debug("scheduling create jobs: {}, interval: {}", createJobs, interval);
			
			getContext().system().scheduler().scheduleOnce(interval, getSelf(), 
				createJobs, getContext().dispatcher(), getSelf());
		} else {
			unhandled(msg);
		}
	}

}
