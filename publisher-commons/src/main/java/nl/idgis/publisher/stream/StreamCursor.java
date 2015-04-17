package nl.idgis.publisher.stream;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Retry;
import nl.idgis.publisher.stream.messages.Stop;
import nl.idgis.publisher.utils.FutureUtils;

public abstract class StreamCursor<T, V extends Item> extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final T t;
	
	protected FutureUtils f;
	
	private V lastItem;
		
	public StreamCursor(T t) {
		this.t = t;
	}
	
	protected abstract boolean hasNext() throws Exception;

	protected abstract CompletableFuture<V> next();
	
	protected void preStartElse() throws Exception {
		
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
		
		f = new FutureUtils(getContext());
		preStartElse();
	}
	
	protected void onReceiveElse(Object msg) throws Exception {
		unhandled(msg);
	}
	
	private static class ItemReceived<T> {
		
		final T item;
		
		final ActorRef sender;
		
		ItemReceived(T item, ActorRef sender) {
			this.item = item;
			this.sender = sender;
		}
		
		T getItem() {
			return item;
		}
		
		ActorRef getSender() {
			return sender;
		}
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof NextItem) {
			log.debug("next");
			if(hasNext()) {
				
				ActorRef sender = getSender(), self = getSelf();				
				next().whenComplete((item, throwable) -> {
					
					if(throwable != null) {
						log.error("next completed exceptionally: {}", throwable);
						
						sender.tell(new Failure(throwable), self);
						self.tell(PoisonPill.getInstance(), self);
					} else {
						self.tell(new ItemReceived<>(item, sender), self);
					}					
				});
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
		} else if (msg instanceof ItemReceived) {
			@SuppressWarnings("unchecked")
			ItemReceived<V> itemReceived = (ItemReceived<V>)msg;
			
			lastItem = itemReceived.getItem();			
			itemReceived.getSender().tell(lastItem, getSelf());
		} else if (msg instanceof Retry) {
			if(lastItem == null) {
				getSender().tell(new Failure(new IllegalStateException("nothing sent yet")), getSelf());
			} else {
				getSender().tell(lastItem, getSelf());
			}
		} else {			
			onReceiveElse(msg);
		}
	}
}
