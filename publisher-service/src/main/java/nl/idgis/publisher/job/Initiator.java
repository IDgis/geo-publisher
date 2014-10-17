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
import akka.japi.Pair;

public class Initiator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef source;
	private final Map<Object, Pair<String, ActorRef>> actorRefs;
	private final FiniteDuration pollInterval, dispatcherTimeout;
	
	private final Map<ActorRef, Object> dispatchers = new HashMap<>();
	
	public Initiator(ActorRef source, Map<Object, Pair<String, ActorRef>> actorRefs, FiniteDuration pollInterval, FiniteDuration dispatcherTimeout) {
		this.source = source;
		this.actorRefs = actorRefs;
		this.pollInterval = pollInterval;
		this.dispatcherTimeout = dispatcherTimeout;
	}
	
	public static class PropsFactory {
		
		private static final FiniteDuration 
			DEFAULT_POLL_INTERVAL = Duration.create(10, TimeUnit.SECONDS),
			DEFAULT_DISPATCHER_TIMEOUT = Duration.create(15, TimeUnit.SECONDS);
		
		final Map<Object, Pair<String, ActorRef>> actorRefs = new HashMap<>();
		
		public PropsFactory add(ActorRef target, String name, Object msg) {
			actorRefs.put(msg, Pair.apply(name, target));
			
			return this;
		}
		
		public Props create(ActorRef source) {
			return create(source, DEFAULT_POLL_INTERVAL, DEFAULT_DISPATCHER_TIMEOUT);
		}
		
		public Props create(ActorRef source, FiniteDuration pollInterval) {
			return create(source, pollInterval, DEFAULT_DISPATCHER_TIMEOUT);
		}
		
		public Props create(ActorRef source, FiniteDuration pollInterval, FiniteDuration dispatcherTimeout) {
			return Props.create(Initiator.class, source,  Collections.unmodifiableMap(actorRefs), pollInterval, dispatcherTimeout);
		}
	}
	
	public static PropsFactory props() {
		return new PropsFactory();
	}
	
	@Override
	public final void preStart() {
		for(Map.Entry<Object, Pair<String, ActorRef>> actorRef : actorRefs.entrySet()) {
			Object msg = actorRef.getKey();
			
			Pair<String, ActorRef> pair = actorRef.getValue();
			
			String name = pair.first();
			ActorRef target = pair.second();
			
			log.debug("starting initiation dispatcher for: " + target);
			
			ActorRef dispatcher = getContext().actorOf(InitiatorDispatcher.props(target, dispatcherTimeout), name);
			source.tell(msg, dispatcher);
			
			dispatchers.put(dispatcher, msg);
		}
	}

	@Override
	public final void onReceive(Object msg) {
		if(msg instanceof Ack) {
			final ActorRef dispatcher = getSender();
			
			if(dispatchers.containsKey(dispatcher)) {				
				final Object fetchMsg = dispatchers.get(dispatcher);
				
				log.debug("scheduling new job retrieval: " + fetchMsg);
				
				ActorSystem system = getContext().system();
				system.scheduler().scheduleOnce(pollInterval, source, fetchMsg, system.dispatcher(), dispatcher);
			} else {
				unhandled(msg);
			}
		} else {
			unhandled(msg);
		}
	}
}
