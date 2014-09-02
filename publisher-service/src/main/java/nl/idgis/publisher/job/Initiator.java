package nl.idgis.publisher.job;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetServiceJobs;
import nl.idgis.publisher.database.messages.JobInfo;
import nl.idgis.publisher.job.messages.Initiate;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.TypedIterable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Initiator extends UntypedActor {
	
	private static final FiniteDuration INTERVAL = Duration.create(10, TimeUnit.SECONDS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final Map<Object, ActorRef> actorRefs;
	
	private ActorRef jobTarget;
	private Iterator<JobInfo> jobItr;
	
	private Iterator<Entry<Object, ActorRef>> actorRefItr;
	
	public Initiator(ActorRef database, Map<Object, ActorRef> actorRefs) {
		this.database = database;
		this.actorRefs = actorRefs;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		Map<Object, ActorRef> actorRefs = new HashMap<>();
		actorRefs.put(new GetHarvestJobs(), harvester);
		actorRefs.put(new GetImportJobs(), loader);
		actorRefs.put(new GetServiceJobs(), service);
		
		return Props.create(Initiator.class, database,  Collections.unmodifiableMap(actorRefs));
	}
	
	private void nextJob() {
		if(jobItr.hasNext()) {
			log.debug("sending job to target");
			
			jobTarget.tell(jobItr.next(), getSelf());
		} else {
			log.debug("all jobs sent");
			
			jobItr = null;			
			nextActorRef();
		}
	}
	
	private void nextActorRef() {
		if(actorRefItr.hasNext()) {
			Entry<Object, ActorRef> actorRefEntry = actorRefItr.next();
			
			jobTarget = actorRefEntry.getValue();					
			Object msg = actorRefEntry.getKey();
			
			log.debug("requesting jobs: " + msg);
			database.tell(msg, getSelf());
		} else {
			actorRefItr = null;
			scheduleInitiate();
		}
	}
	
	private void scheduleInitiate() {
		log.debug("scheduling next initiate message");
		
		getContext().system().scheduler().scheduleOnce(INTERVAL, getSelf(), new Initiate(), getContext().dispatcher(), getSelf());
	}
	
	@Override
	public final void preStart() {
		getSelf().tell(new Initiate(), getSelf());
	}

	@Override
	public final void onReceive(Object msg) {
		if(msg instanceof Initiate) {
			log.debug("initiating jobs");
			
			actorRefItr = actorRefs.entrySet().iterator();			
			nextActorRef();
		} else if(msg instanceof TypedIterable) {
			TypedIterable<?> typedIterable = (TypedIterable<?>)msg;
			if(typedIterable.contains(JobInfo.class)) {			
				log.debug("job list received");			
				
				jobItr = typedIterable.cast(JobInfo.class).iterator();
				nextJob();
			} else {
				unhandled(msg);
			}
		}  else if(msg instanceof Ack) {
			log.debug("job delivered");
			
			nextJob();		
		} else {		
			unhandled(msg);
		}
	}
}
