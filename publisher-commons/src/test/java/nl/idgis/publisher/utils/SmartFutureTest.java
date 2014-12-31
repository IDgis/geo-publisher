package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;

public class SmartFutureTest {
	
	static final Duration WAIT_AT_MOST = Duration.create(1, TimeUnit.SECONDS);
	
	ExecutionContext executionContext;
	
	@Before
	public void setup() {
		executionContext = ActorSystem.create().dispatcher();
	}

	@Test
	public void testMap() throws Exception {		
		SmartFuture<Integer> future = new SmartFuture<>(Futures.successful(42), executionContext);		
		
		assertEquals("42", future.map((Integer i) -> i.toString()).get(WAIT_AT_MOST));
	}
	
	@Test(expected=IllegalStateException.class)
	public void testMapFailure() throws Exception {
		SmartFuture<Integer> future = new SmartFuture<>(Futures.successful(42), executionContext);	
		
		future.map(i -> {
			throw new IllegalStateException();	
		}).get(WAIT_AT_MOST);
	}
	
	@Test
	public void testFlatMap() throws Exception {		
		SmartFuture<Integer> future = new SmartFuture<>(Futures.successful(42), executionContext);		
		
		assertEquals("42", future.flatMap((Integer i) -> 
			new SmartFuture<>(Futures.successful(i.toString()), executionContext)).get(WAIT_AT_MOST));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFlatMapSameExecutionContext() throws Exception {
		SmartFuture<Integer> future = new SmartFuture<>(Futures.successful(42), executionContext);
		
		ExecutionContext differentExecutionContext = ActorSystem.create().dispatcher();
		
		assertEquals("42", future.flatMap((Integer i) -> 
			new SmartFuture<>(Futures.successful(i.toString()), differentExecutionContext)).get(WAIT_AT_MOST));
	}
	
	@Test
	public void testMapValue() throws Exception {
		SmartFuture<Integer> future = new SmartFuture<>(Futures.successful(42), executionContext);
		assertEquals(Integer.valueOf(47), future.mapValue(47).get(WAIT_AT_MOST));
	}
	
	@Test
	public void testFailure() throws Exception {
		Promise<Boolean> testPromise = Futures.promise();
		
		SmartFuture<Integer> future = new SmartFuture<>(Futures.failed(new IllegalStateException()), executionContext);
		future.onFailure((Throwable t) -> {
			testPromise.success(true);
		});
		
		assertTrue(Await.result(testPromise.future(), WAIT_AT_MOST));
	}
	
	@Test
	public void testCast() throws Exception {
		Promise<Boolean> testPromise = Futures.promise();
		
		SmartFuture<Object> objectFuture = new SmartFuture<>(Futures.<Object>successful("Hello world!"), executionContext);
		
		objectFuture.cast(String.class).map((String s) -> {
			testPromise.success(true);
			
			return null;
		});
		
		assertTrue(Await.result(testPromise.future(), WAIT_AT_MOST));
	}
}
