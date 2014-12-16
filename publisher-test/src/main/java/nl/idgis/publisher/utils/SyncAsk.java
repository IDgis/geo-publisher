package nl.idgis.publisher.utils;

import static org.junit.Assert.fail;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSelection;
import akka.util.Timeout;

import nl.idgis.publisher.utils.Ask.Response;

public class SyncAsk {

	public static Response askResponse(ActorRefFactory refFactory, ActorRef actorRef, Object msg, Timeout askTimeout, Duration awaitDuration) throws Exception {
		return Await.result(Ask.askResponse(refFactory, actorRef, msg, askTimeout), awaitDuration);
	}
	
	public static Response askResponse(ActorRefFactory refFactory, ActorSelection actorSelection, Object msg, Timeout askTimeout, Duration awaitDuration) throws Exception {
		return Await.result(Ask.askResponse(refFactory, actorSelection, msg, askTimeout), awaitDuration);
	}
	
	public static <T> T ask(ActorRefFactory refFactory, ActorRef actorRef, Object msg, Timeout askTimeout, Duration awaitDuration, Class<T> expected) throws Exception {
		return result(expected, askResponse(refFactory, actorRef, msg, askTimeout, awaitDuration).getMessage());
	}
	
	public static <T> T ask(ActorRefFactory refFactory, ActorSelection actorSelection, Object msg, Timeout askTimeout, Duration awaitDuration, Class<T> expected) throws Exception {
		return result(expected, askResponse(refFactory, actorSelection, msg, askTimeout, awaitDuration).getMessage());
	}
	
	public static <T> T result(Class<T> expected, Object result) throws Exception {
		if(expected.isInstance(result)) {
			return expected.cast(result);
		} else {
			fail("expected: " + expected.getCanonicalName() + " received: " + result.getClass().getCanonicalName());
			return null;
		}
	}
}
