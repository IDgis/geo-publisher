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
import nl.idgis.publisher.stream.messages.Stop;
import nl.idgis.publisher.utils.FutureUtils;

public abstract class StreamCursor<T, V extends Item> extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final T t;
	
	protected FutureUtils f;
		
	public StreamCursor(T t) {
		this.t = t;
	}
	
	protected abstract boolean hasNext() throws Exception;

	protected abstract CompletableFuture<V> next();
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
		
		f = new FutureUtils(getContext().dispatcher());
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof NextItem) {
			log.debug("next");
			if(hasNext()) {
				
				ActorRef sender = getSender(), self = getSelf();				
				next().whenComplete((v, t) -> {
					
					if(t != null) {
						log.error("next completed exceptionally: {}", t);
						
						sender.tell(new Failure(t), self);
						self.tell(PoisonPill.getInstance(), self);
					} else {
						sender.tell(v, self);
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
		} else {			
			unhandled(msg);
		}
	}
}
