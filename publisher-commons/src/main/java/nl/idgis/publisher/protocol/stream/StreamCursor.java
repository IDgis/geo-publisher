package nl.idgis.publisher.protocol.stream;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.pattern.Patterns;

public abstract class StreamCursor<T, V extends Item> extends UntypedActor {
	
	protected final T t;
	
	public StreamCursor(T t) {
		this.t = t;
	}
	
	protected abstract boolean hasNext() throws Exception;

	protected abstract Future<V> next();

	@Override
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof NextItem) {
			if(hasNext()) {
				next().onComplete(new OnComplete<V>() {
					
					private final ActorRef sender = getSender();

					@Override
					public void onComplete(Throwable t, V v) throws Throwable {
						if(t != null) {
							sender.tell(new Failure(t), getSelf());
						} else {
							sender.tell(v, getSelf());
						}
					}
				}, getContext().system().dispatcher());
			} else {
				getSender().tell(new End(), getSelf());
				getContext().stop(getSelf());
			}
		} else if (msg instanceof Stop) {			
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
