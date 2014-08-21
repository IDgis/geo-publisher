package nl.idgis.publisher.job;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.JobInfo;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Initiator extends Scheduled {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final Map<Object, ActorRef> actorRefs;
	
	public Initiator(ActorRef database, ActorRef harvester, ActorRef loader) {
		this.database = database;
		
		Map<Object, ActorRef> actorRefs = new HashMap<>();
		actorRefs.put(new GetHarvestJobs(), harvester);
		actorRefs.put(new GetImportJobs(), loader);
		
		this.actorRefs = Collections.unmodifiableMap(actorRefs);
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader) {
		return Props.create(Initiator.class, database, harvester, loader);
	}

	@Override
	protected void doInitiate() {
		log.debug("initiating jobs");
		
		for(final Map.Entry<Object, ActorRef> actorRefEntry : actorRefs.entrySet()) {
			final Object msg = actorRefEntry.getKey();
			final ActorRef actorRef = actorRefEntry.getValue();
			
			log.debug("querying for jobs: " + msg);
			
			Patterns.ask(database, msg, 15000)
				.onSuccess(new OnSuccess<Object>() {
					
					@Override
					@SuppressWarnings("unchecked")
					public void onSuccess(Object msg) throws Throwable {
						List<? extends JobInfo> jobs = (List<? extends JobInfo>)msg;
						for(JobInfo job : jobs) {
							log.debug("dispatching job: " + job);
							
							actorRef.tell(job, getSelf());
						}
					}
					
				}, getContext().dispatcher());			
		}
	}
}
