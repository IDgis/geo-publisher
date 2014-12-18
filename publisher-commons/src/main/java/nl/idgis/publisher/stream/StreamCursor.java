package nl.idgis.publisher.stream;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;

import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public abstract class StreamCursor<T, V extends Item> extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final T t;
		
	public StreamCursor(T t) {
		this.t = t;
	}
	
	protected abstract boolean hasNext() throws Exception;

	protected abstract Future<V> next();
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof NextItem) {
			log.debug("next");
			if(hasNext()) {
				final ActorRef sender = getSender();
				
				next().onComplete(new OnComplete<V>() { 

					@Override
					public void onComplete(Throwable t, V v) throws Throwable {
						if(t != null) {
							log.warning("next returned exception: " + t.getMessage());
							sender.tell(new Failure(t), getSelf());
						} else {
							sender.tell(v, getSelf());
						}
					}
				}, getContext().system().dispatcher());
			} else {
				log.debug("end");
				getSender().tell(new End(), getSelf());
				getContext().stop(getSelf());
			}
		} else if (msg instanceof Stop) {
			log.debug("stopped");
			getContext().stop(getSelf());
		} else if (msg instanceof ReceiveTimeout){
			log.error("timeout");
			getContext().stop(getSelf());
		} else {			
			unhandled(msg);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Future<V> askActor(ActorRef actorRef, Object msg, long timeout) {
		return Patterns.ask(actorRef, msg, timeout).map(new Mapper<Object, V>() {
			
			@Override
			public V checkedApply(Object parameter) throws Throwable {
				if(parameter instanceof Failure) {
					throw ((Failure)parameter).getCause();
				}
				
				return (V)parameter;
			}
			
		}, getContext().system().dispatcher());
	}
}
