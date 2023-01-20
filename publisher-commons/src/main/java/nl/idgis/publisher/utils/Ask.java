package nl.idgis.publisher.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import scala.concurrent.Future;
import scala.concurrent.Promise;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

public final class Ask extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Promise<AskResponse<Object>> promise;
	
	private final Timeout timeout;

	private final boolean ignoreBusy;
	
	public Ask(Promise<AskResponse<Object>> promise, Timeout timeout, boolean ignoreBusy) {
		this.promise = promise;
		this.timeout = timeout;
		this.ignoreBusy = ignoreBusy;
	}

	private static Props props(Promise<AskResponse<Object>> promise, Timeout timeout, boolean ignoreBusy) {
		return Props.create(Ask.class, promise, timeout, ignoreBusy);
	}

	public static Future<Object> ask(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, long timeoutMillis)  {
		return ask(refFactory, actorSelection, message, timeoutMillis, false);
	}

	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actorRef, Object message, long timeoutMillis)  {
		return ask(refFactory, actorRef, message, timeoutMillis, false);
	}

	public static Future<AskResponse<Object>> askWithSender(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, Timeout timeout) {
		return askWithSender(refFactory, actorSelection, message, timeout, false);
	}

	public static Future<AskResponse<Object>> askWithSender(ActorRefFactory refFactory, ActorRef actorRef, Object message, Timeout timeout) {
		return askWithSender(refFactory, actorRef, message, timeout, false);
	}

	public static Future<Object> ask(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, Timeout timeout)  {
		return ask(refFactory, actorSelection, message, timeout, false);
	}

	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actorRef, Object message, Timeout timeout)  {
		return ask(refFactory, actorRef, message, timeout, false);
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, long timeoutMillis, boolean ignoreBusy)  {
		return ask(refFactory, actorSelection, message, new Timeout(timeoutMillis, TimeUnit.MILLISECONDS), ignoreBusy);
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actorRef, Object message, long timeoutMillis, boolean ignoreBusy)  {
		return ask(refFactory, actorRef, message, new Timeout(timeoutMillis, TimeUnit.MILLISECONDS), ignoreBusy);
	}
	
	public static Future<AskResponse<Object>> askWithSender(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, Timeout timeout, boolean ignoreBusy) {
		Promise<AskResponse<Object>> promise = Futures.promise();
		
		actorSelection.tell(message, refFactory.actorOf(Ask.props(promise, timeout, ignoreBusy)));
		
		return promise.future();
	}
	
	public static Future<AskResponse<Object>> askWithSender(ActorRefFactory refFactory, ActorRef actorRef, Object message, Timeout timeout, boolean ignoreBusy) {
		Promise<AskResponse<Object>> promise = Futures.promise();
		
		actorRef.tell(message, refFactory.actorOf(Ask.props(promise, timeout, ignoreBusy)));
		
		return promise.future();
	}
	
	private static Future<Object> getMessage(ActorRefFactory refFactory, Future<AskResponse<Object>> response) {
		return response.map(new Mapper<AskResponse<Object>, Object>() {
			
			@Override
			public Object apply(AskResponse<Object> response) {
				return response.getMessage();
			}
		}, refFactory.dispatcher());
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, Timeout timeout, boolean ignoreBusy)  {
		return getMessage(refFactory, askWithSender(refFactory, actorSelection, message, timeout, ignoreBusy));
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actorRef, Object message, Timeout timeout, boolean ignoreBusy)  {
		return getMessage(refFactory, askWithSender(refFactory, actorRef, message, timeout, ignoreBusy));
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(timeout.duration());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.debug("timeout");
			
			promise.failure(new TimeoutException("ask timeout: " + timeout.toString()));
		} else {
			if (ignoreBusy && msg instanceof Busy) {
				log.debug("busy received");
				return;
			}

			log.debug("answer received");
			
			promise.success(new AskResponse<>(msg, getSender()));
		}
		
		getContext().stop(self());
	}
}
