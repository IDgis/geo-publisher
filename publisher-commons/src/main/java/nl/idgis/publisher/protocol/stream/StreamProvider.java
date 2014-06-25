package nl.idgis.publisher.protocol.stream;

import java.util.Iterator;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.japi.Procedure;

public abstract class StreamProvider<T, U extends Iterator<T>, V extends Start, W extends Item>
		extends UntypedActor {

	protected abstract U start(V msg);

	protected void stop(U i) {
		
	}

	protected abstract void process(T t, StreamHandle<W> handle) throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Start) {
			getContext().become(active(start((V) msg)), false);
		} else {
			unhandled(msg);
		}
	}

	private Procedure<Object> active(final U i) {
		return new Procedure<Object>() {
			
			{
				nextItem();
			}

			void nextItem() {
				if (i.hasNext()) {
					try {
						process(i.next(), new StreamHandle<W>() {
							
							private final ActorRef sender = getSender();

							@Override
							public void item(W t) {
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
