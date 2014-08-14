package nl.idgis.publisher.utils;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import scala.concurrent.Future;
import scala.concurrent.Promise;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

public final class Ask extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Promise<Object> promise;
	private final Timeout timeout;
	
	private Cancellable timeoutCancellable;
	
	private static class Stop implements Serializable {
		
		private static final long serialVersionUID = -1585596166238409731L;		
	}
	
	public Ask(Promise<Object> promise, Timeout timeout) {
		this.promise = promise;
		this.timeout = timeout;
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actor, Object message, long timeoutMillis)  {
		return ask(refFactory, actor, message, new Timeout(timeoutMillis, TimeUnit.MILLISECONDS));
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actor, Object message, Timeout timeout)  {
		Promise<Object> promise = Futures.promise();
		
		actor.tell(message, refFactory.actorOf(Props.create(Ask.class, promise, timeout))); 
		
		return promise.future();
	}
	
	@Override
	public void preStart() throws Exception {
		log.debug("waiting for answer");
		
		timeoutCancellable = getContext().system().scheduler().scheduleOnce(timeout.duration(), 
			getSelf(), new Stop(), getContext().dispatcher(), getSelf());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Stop) {
			log.debug("timeout");
			
			promise.failure(new TimeoutException());
		} else {
			log.debug("answer received");
			
			timeoutCancellable.cancel();
			promise.success(msg);
		}
		
		getContext().stop(self());
	}
}
