package nl.idgis.publisher.protocol.stream;

import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.Mapper;
import akka.dispatch.OnComplete;
import akka.japi.Procedure;
import akka.pattern.Patterns;

public abstract class StreamProvider<T, U extends Start, V extends Item>
		extends UntypedActor {

	protected abstract T start(U msg) throws Exception;

	protected void stop(T i) throws Exception {
		
	}
	
	protected abstract boolean hasNext(T u) throws Exception;

	protected abstract Future<V> next(T u);
	
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

	@Override
	@SuppressWarnings("unchecked")
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Start) {
			getContext().become(active(start((U) msg)), false);
		} else {
			unhandled(msg);
		}
	}

	private Procedure<Object> active(final T i) throws Exception {
		return new Procedure<Object>() {
			
			{
				nextItem();
			}

			void nextItem() throws Exception {
				if (hasNext(i)) {										
					next(i).onComplete(new OnComplete<V>() {
						
						private final ActorRef sender = getSender();

						@Override
						public void onComplete(Throwable t, V v) throws Throwable {
							if(t != null) {
								sender.tell(new Failure(t), sender);
							} else {
								sender.tell(v, getSelf());
							}
						}
					}, getContext().system().dispatcher());
				} else {
					stop(i);
					getSender().tell(new End(), getSelf());
					getContext().unbecome();
				}
			}

			@Override
			public void apply(Object msg) throws Exception {
				if (msg instanceof NextItem) {
					nextItem();
				} else if (msg instanceof Stop) {
					stop(i);
					getContext().unbecome();
				} else {
					unhandled(msg);
				}
			}
		};
	}
}
