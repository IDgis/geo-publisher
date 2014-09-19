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

	public static <T> T askAssert(ActorRef actorRef, Object msg, Class<T> resultType) throws Exception {
		Object result = ask(actorRef, msg);		
		assertTrue("Unexpected result type: " + result.getClass(), resultType.isInstance(result));
		return resultType.cast(result);
	}
	
	public static Object ask(ActorRef actorRef, Object msg) throws Exception {
		try {
			Future<Object> future = Patterns.ask(actorRef, msg, 5000);
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
