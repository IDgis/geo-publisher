package nl.idgis.publisher.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeUnit;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.pattern.Patterns;

public class TestPatterns {
	
	private static long DEFAULT_TIMEOUT = 5000;
	
	public static <T> T askAssert(ActorRef actorRef, Object msg, Class<T> resultType) throws Exception {
		return askAssert(actorRef, msg, DEFAULT_TIMEOUT, resultType);
	}

	public static <T> T askAssert(ActorRef actorRef, Object msg, long timeout, Class<T> resultType) throws Exception {
		Object result = ask(actorRef, msg, timeout);		
		assertTrue("Unexpected result type: " + result.getClass(), resultType.isInstance(result));
		return resultType.cast(result);
	}
	
	public static Object ask(ActorRef actorRef, Object msg) throws Exception {
		return ask(actorRef, msg, DEFAULT_TIMEOUT);
	}
	
	public static Object ask(ActorRef actorRef, Object msg, long timeout) throws Exception {
		try {
			Future<Object> future = Patterns.ask(actorRef, msg, timeout);
			return Await.result(future, Duration.create(5, TimeUnit.MINUTES));
		} catch(Throwable t) {
			fail(t.getMessage());
			return null;
		}
	}
	
	public static  <T> T askAssert(ActorSelection actorSelection, Object msg, Class<T> resultType) throws Exception {
		Object result = ask(actorSelection, msg);		
		assertTrue("Unexpected result type: " + result.getClass(), resultType.isInstance(result));
		return resultType.cast(result);
	}
	
	public static  Object ask(ActorSelection actorSelection, Object msg) throws Exception {
		try {
			Future<Object> future = Patterns.ask(actorSelection, msg, 5000);
			return Await.result(future, Duration.create(5, TimeUnit.MINUTES));
		} catch(Throwable t) {
			fail(t.getMessage());
			return null;
		}
	}
}
