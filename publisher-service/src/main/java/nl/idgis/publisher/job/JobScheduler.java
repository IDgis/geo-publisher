package nl.idgis.publisher.job;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.quartz.CronExpression;

import nl.idgis.publisher.job.creator.Creator;
import nl.idgis.publisher.job.creator.messages.CreateHarvestJobs;
import nl.idgis.publisher.job.creator.messages.CreateImportJobs;
import nl.idgis.publisher.job.creator.messages.CreateJobs;
import nl.idgis.publisher.job.manager.messages.GetHarvestJobs;
import nl.idgis.publisher.job.manager.messages.GetImportJobs;
import nl.idgis.publisher.job.manager.messages.GetRemoveJobs;
import nl.idgis.publisher.job.manager.messages.GetServiceJobs;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.Either;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

public class JobScheduler extends UntypedActor {	
	
	protected static final String ON_THE_HOUR = "* 0 * * * ?";
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader, provisioningManager, serviceManager;
	
	private final ActorRef jobManager; 
	
	private ActorRef jobCreator;
	
	private FutureUtils f;
	
	private Map<CreateJobs, Either<FiniteDuration, CronExpression>> createJobsIntervals;
	
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
	
	public JobScheduler(ActorRef database, ActorRef jobManager, ActorRef harvester, ActorRef loader, ActorRef provisioningManager, ActorRef serviceManager) {
		this.database = database;
		this.jobManager = jobManager;
		this.harvester = harvester;
		this.loader = loader;
		this.provisioningManager = provisioningManager;
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef database, ActorRef jobManager, ActorRef harvester, ActorRef loader, ActorRef provisioningManager, ActorRef serviceManager) {
		return Props.create(JobScheduler.class, database, jobManager, harvester, loader, provisioningManager, serviceManager);
	}
	
	@Override
	public void preStart() throws Exception {
		jobCreator = getContext().actorOf(Creator.props(jobManager, database, serviceManager), "creator");
		
		getContext().actorOf(
			Initiator.props()
				.add(harvester, "harvester", new GetHarvestJobs())
				.add(loader, "import", new GetImportJobs())
				.add(loader, "remove", new GetRemoveJobs())
				.add(provisioningManager, "service", new GetServiceJobs())
				.create(jobManager, jobCreator), 
			"initiator");
		
		createJobsIntervals = new HashMap<>();
		createJobsIntervals.put(new CreateHarvestJobs(), Either.right(new CronExpression(ON_THE_HOUR)));
		createJobsIntervals.put(new CreateImportJobs(), Either.left(Duration.apply(10, TimeUnit.SECONDS)));
		
		createJobsIntervals.keySet().stream()
			.forEach(msg -> getSelf().tell(msg, getSelf()));
		
		f = new FutureUtils(getContext());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof CreateJobs) {
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
			FiniteDuration interval = createJobsIntervals.get(createJobs).mapRight(this::toInterval);
			
			log.debug("scheduling create jobs: {}, interval: {}", createJobs, interval);
			
			getContext().system().scheduler().scheduleOnce(interval, getSelf(), 
				createJobs, getContext().dispatcher(), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	protected FiniteDuration toInterval(CronExpression cronExpression) {
		return toInterval(cronExpression, new Date());
	}
	
	protected FiniteDuration toInterval(CronExpression cronExpression, Date now) {
		log.debug("computing interval based on cron expression: {}", cronExpression.getCronExpression());
		
		Date next = cronExpression.getNextValidTimeAfter(now);		
		
		if(log.isDebugEnabled()) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			log.debug("next time: {}", dateFormat.format(next));
		}
		
		return Duration.apply(next.getTime() - now.getTime(), TimeUnit.MILLISECONDS);
	}

}
