package nl.idgis.publisher.job;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetServiceJobs;
import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Initiator extends UntypedActor {
	
	private static final FiniteDuration DEFAULT_INTERVAL = Duration.create(10, TimeUnit.SECONDS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final Map<Object, ActorRef> actorRefs;
	private final FiniteDuration interval;
	
	private final Map<ActorRef, Object> dispatchers = new HashMap<>();
	
	public Initiator(ActorRef database, Map<Object, ActorRef> actorRefs, FiniteDuration interval) {
		this.database = database;
		this.actorRefs = actorRefs;
		this.interval = interval;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		return props(database, harvester, loader, service, DEFAULT_INTERVAL);
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service, FiniteDuration interval) {
		Map<Object, ActorRef> actorRefs = new HashMap<>();
		actorRefs.put(new GetHarvestJobs(), harvester);
		actorRefs.put(new GetImportJobs(), loader);
		actorRefs.put(new GetServiceJobs(), service);
		
		return Props.create(Initiator.class, database,  Collections.unmodifiableMap(actorRefs), interval);
	}
	
	@Override
	public final void preStart() {
		for(Map.Entry<Object, ActorRef> actorRef : actorRefs.entrySet()) {
			Object msg = actorRef.getKey();
			ActorRef target = actorRef.getValue();
			
			log.debug("starting initiation dispatcher for: " + target);
			
			ActorRef dispatcher = getContext().actorOf(InitiatorDispatcher.props(target));
			database.tell(msg, dispatcher);
			
			dispatchers.put(dispatcher, msg);
		}
	}

	@Override
	public final void onReceive(Object msg) {
		if(msg instanceof Ack) {
			final ActorRef dispatcher = getSender();
			
			if(dispatchers.containsKey(dispatcher)) {				
				final Object databaseMsg = dispatchers.get(dispatcher);
				
				log.debug("scheduling new job retrieval: " + databaseMsg);
				
				ActorSystem system = getContext().system();
				system.scheduler().scheduleOnce(interval, database, databaseMsg, system.dispatcher(), dispatcher);
			} else {
				unhandled(msg);
			}
		} else {
			unhandled(msg);
		}
	}
}
