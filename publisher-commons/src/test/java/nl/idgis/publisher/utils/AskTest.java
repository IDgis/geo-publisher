package nl.idgis.publisher.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.util.Timeout;

public class AskTest {
	
	final Timeout timeout = Timeout.apply(1, TimeUnit.SECONDS);
	
	final Duration duration = Duration.apply(2, TimeUnit.SECONDS);
	
	public static class Incrementer extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			getSender().tell(((Integer)msg) + 1, getSelf());
		}
	}
	
	public static class NoResponse extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			
		}
		
	}
	
	ActorSystem actorSystem;
	
	ActorRef incrementer, noResponse;
	
	@Before
	public void actors() {
		actorSystem = ActorSystem.create();
		
		incrementer = actorSystem.actorOf(Props.create(Incrementer.class));
		noResponse = actorSystem.actorOf(Props.create(NoResponse.class));
	}
	
	@Test
	public void testAsk() throws Exception {				
		assertEquals(3, (int)Await.result(Ask.ask(actorSystem, incrementer, 2, timeout), duration));
	}
	
	@Test
	public void testAskResponse() throws Exception {
		AskResponse<Object> response = Await.result(Ask.askWithSender(actorSystem, incrementer, 3, timeout), duration);
		assertEquals(incrementer, response.getSender());
		assertEquals(4, (int)response.getMessage());
	}
	
	@Test
	public void testTimeout() throws Throwable {
		final Promise<Object> testPromise = Futures.promise();
		
		Ask.ask(actorSystem, noResponse, "A message", timeout)
			.onComplete(new OnComplete<Object>() {

				@Override
				public void onComplete(Throwable t, Object o) throws Throwable {
					try {
						assertNotNull(t);
						assertTrue(t instanceof TimeoutException);
						
						testPromise.success(true);
					} catch(Throwable testThrowable) {
						testPromise.failure(testThrowable);
					}
				}
				
			}, actorSystem.dispatcher());
		
		try {
			Await.result(testPromise.future(), Duration.create(5, TimeUnit.SECONDS));
		} catch(ExecutionException e) {
			throw e.getCause();
		}
	}
}
