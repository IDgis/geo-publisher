package nl.idgis.publisher.stream;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Start;
import nl.idgis.publisher.stream.messages.Stop;
import nl.idgis.publisher.stream.messages.Unavailable;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public abstract class StreamConverter extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef sender, producer;
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.apply(30, TimeUnit.SECONDS));
	}
	
	protected boolean includeInStream(Object msg) {
		return true;
	}
	
	private Procedure<Object> converting(Item<?> original, ActorRef sender) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("converted item received: {}", msg);
				
				if(includeInStream(msg)) {
					log.debug("sending to consumer");
					sender.tell(new Item<>(original.getSequenceNumber(), msg), getSelf());
				} else {
					log.debug("skipping, requesting next item from producer");
					producer.tell(new NextItem(), getSelf());
				}
				
				getContext().become(receive());
			}
			
		};
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof Start) {
			log.debug("start");
			
			sender = getSender();
			start((Start)msg);
		} else if(msg instanceof End) {
			log.debug("end");
			
			sender.tell(msg, getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Item) {
			log.debug("item");
			
			producer = getSender();
			Item<?> item = (Item<?>)msg;
			if(convert(item.getContent())) {
				getContext().become(converting(item, sender));
			} else {
				unhandled(msg);
			}
		} else if(msg instanceof NextItem) {
			log.debug("next item");
			
			sender = getSender();
			producer.tell(msg, getSelf());
		} else if(msg instanceof Stop) {
			log.debug("stop");
			
			producer.tell(msg, getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof ReceiveTimeout){
			log.error("timeout");
			
			getContext().stop(getSelf());
		} else if(msg instanceof Unavailable) {
			log.warning("unavailable");
			
			sender.tell(msg, getSelf());
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	protected abstract void start(Start msg) throws Exception;
	
	protected abstract boolean convert(Object msg) throws Exception;
	
}
