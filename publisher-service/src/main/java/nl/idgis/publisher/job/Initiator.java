package nl.idgis.publisher.job;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Initiator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final Map<Object, ActorRef> actorRefs;
	private final FiniteDuration pollInterval, dispatcherTimeout;
	
	private final Map<ActorRef, Object> dispatchers = new HashMap<>();
	
	public Initiator(ActorRef database, Map<Object, ActorRef> actorRefs, FiniteDuration pollInterval, FiniteDuration dispatcherTimeout) {
		this.database = database;
		this.actorRefs = actorRefs;
		this.pollInterval = pollInterval;
		this.dispatcherTimeout = dispatcherTimeout;
	}
	
	public static class PropsFactory {
		
		private static final FiniteDuration 
			DEFAULT_POLL_INTERVAL = Duration.create(10, TimeUnit.SECONDS),
			DEFAULT_DISPATCHER_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);
		
		final Map<Object, ActorRef> actorRefs = new HashMap<>();
		
		public PropsFactory add(ActorRef target, Object msg) {
			actorRefs.put(msg, target);
			
			return this;
		}
		
		public Props create(ActorRef database) {
			return create(database, DEFAULT_POLL_INTERVAL, DEFAULT_DISPATCHER_TIMEOUT);
		}
		
		public Props create(ActorRef database, FiniteDuration pollInterval) {
			return create(database, pollInterval, DEFAULT_DISPATCHER_TIMEOUT);
		}
		
		public Props create(ActorRef database, FiniteDuration pollInterval, FiniteDuration dispatcherTimeout) {
			return Props.create(Initiator.class, database,  Collections.unmodifiableMap(actorRefs), pollInterval, dispatcherTimeout);
		}
	}
	
	public static PropsFactory props() {
		return new PropsFactory();
	}
	
	@Override
	public final void preStart() {
		for(Map.Entry<Object, ActorRef> actorRef : actorRefs.entrySet()) {
			Object msg = actorRef.getKey();
			ActorRef target = actorRef.getValue();
			
			log.debug("starting initiation dispatcher for: " + target);
			
			ActorRef dispatcher = getContext().actorOf(InitiatorDispatcher.props(target, dispatcherTimeout));
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
				system.scheduler().scheduleOnce(pollInterval, database, databaseMsg, system.dispatcher(), dispatcher);
			} else {
				unhandled(msg);
			}
		} else {
			unhandled(msg);
		}
	}
}
