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
	
	private final Promise<Response> promise;
	
	private final Timeout timeout;
		
	public static class Response {
		
		private final ActorRef sender;
		
		private final Object message;
		
		public Response(ActorRef sender, Object message) {
			this.sender = sender;
			this.message = message;
		}
		
		public ActorRef getSender() {
			return sender;
		}
		
		public Object getMessage() {
			return message;
		}
	}
	
	public Ask(Promise<Response> promise, Timeout timeout) {
		this.promise = promise;
		this.timeout = timeout;
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, long timeoutMillis)  {
		return ask(refFactory, actorSelection, message, new Timeout(timeoutMillis, TimeUnit.MILLISECONDS));
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actorRef, Object message, long timeoutMillis)  {
		return ask(refFactory, actorRef, message, new Timeout(timeoutMillis, TimeUnit.MILLISECONDS));
	}
	
	public static Future<Response> askResponse(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, Timeout timeout) {
		Promise<Response> promise = Futures.promise();
		
		actorSelection.tell(message, refFactory.actorOf(Props.create(Ask.class, promise, timeout)));
		
		return promise.future();
	}
	
	public static Future<Response> askResponse(ActorRefFactory refFactory, ActorRef actorRef, Object message, Timeout timeout) {
		Promise<Response> promise = Futures.promise();
		
		actorRef.tell(message, refFactory.actorOf(Props.create(Ask.class, promise, timeout)));
		
		return promise.future();
	}
	
	private static Future<Object> getMessage(ActorRefFactory refFactory, Future<Response> response) {
		return response.map(new Mapper<Response, Object>() {
			
			@Override
			public Object apply(Response response) {
				return response.getMessage();
			}
		}, refFactory.dispatcher());
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorSelection actorSelection, Object message, Timeout timeout)  {
		return getMessage(refFactory, askResponse(refFactory, actorSelection, message, timeout));
	}
	
	public static Future<Object> ask(ActorRefFactory refFactory, ActorRef actorRef, Object message, Timeout timeout)  {
		return getMessage(refFactory, askResponse(refFactory, actorRef, message, timeout));
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
			log.debug("answer received");
			
			promise.success(new Response(getSender(), msg));
		}
		
		getContext().stop(self());
	}
}
