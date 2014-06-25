package nl.idgis.publisher.protocol.stream;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.japi.Procedure;

public abstract class StreamProvider<T, U extends Start, V extends Item>
		extends UntypedActor {

	protected abstract T start(U msg);

	protected void stop(T i) {
		
	}
	
	protected abstract boolean hasNext(T u);

	protected abstract void next(T u, StreamHandle<V> handle) throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Start) {
			getContext().become(active(start((U) msg)), false);
		} else {
			unhandled(msg);
		}
	}

	private Procedure<Object> active(final T i) {
		return new Procedure<Object>() {
			
			{
				nextItem();
			}

			void nextItem() {
				if (hasNext(i)) {
					try {
						next(i, new StreamHandle<V>() {
							
							private final ActorRef sender = getSender();

							@Override
							public void item(V t) {
								sender.tell(t, getSelf());
							}
							
							@Override
							public void failure(String message) {
								sender.tell(new Failure(message), sender);
							}
						});
					} catch (Exception e) {
						getSender()
								.tell(new Failure(e.getMessage()), getSelf());
						getContext().unbecome();
					}
				} else {
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
