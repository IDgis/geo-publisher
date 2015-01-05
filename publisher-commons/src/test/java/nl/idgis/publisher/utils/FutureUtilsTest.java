package nl.idgis.publisher.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FutureUtilsTest {
	
	private FutureUtils f;
	
	private Promise<Object> testPromise;
	
	@Before
	public void setUp() {
		
		ActorSystem system = ActorSystem.create();
		f = new FutureUtils(system.dispatcher());
		
		testPromise = Futures.promise();
	}
	
	@After
	public void doAssert() throws Throwable {
		try {
			Await.result(testPromise.future(), Duration.create(1, TimeUnit.SECONDS));
		} catch(ExecutionException e) {
			throw e.getCause();
		}
	}

	@Test
	public void testCollector() throws Throwable {
		
		f
			.collect(f.successful("Hello world!"))			
			.collect(f.successful(42))
			.map((String s, Integer i) -> {
				try {
					assertEquals("Hello world!", s);
					assertEquals(new Integer(42), i);
					testPromise.success(true);
				} catch(Throwable t) {
					testPromise.failure(t);
				}
				
				return null;
			});
	}
	
	@Test
	public void testCollectFailure() throws Throwable {
		f
			.collect(f.successful("Hello world!"))			
			.collect(f.failed(new Exception("Failure")))
			.map((String s, Object o) -> {
				try {
					fail("result received");						
				} catch(Throwable t) {
					testPromise.failure(t);
				}
				
				return null;
			}).exceptionally(t -> testPromise.success(true));		
	}
	
	@Test
	public void testCollectReturnValue() {
		f
			.collect(		
				f
					.collect(f.successful("Hello world!"))
					.collect(f.successful(42))
					.map((String s, Integer i) -> {						
						return 47;
					}))
			.map((Integer i) -> {
				try {
					assertEquals(new Integer(47), i);
					testPromise.success(true);
				} catch(Throwable t) {
					testPromise.failure(t);
				}
				
				return null;
			});
	}
	
	@Test
	public void testCollectFlatReturnValue() {
		f
			.collect(		
				f
					.collect(f.successful("Hello world!"))
					.collect(f.successful(42))
					.flatMap((String s, Integer i) -> {
						return f.successful(47);						
					}))
			.map((Integer i) -> {
				try {
					assertEquals(new Integer(47), i);
					testPromise.success(true);
				} catch(Throwable t) {
					testPromise.failure(t);
				}
				
				return null;
			});
	}
	
	@Test
	public void testMap() throws Exception {
		Map<String, CompletableFuture<String>> input = new HashMap<>();
		
		input.put("foo", f.successful("bar"));
		
		CompletableFuture<Map<String, String>> outputFuture = f.map(input);
		assertNotNull(outputFuture);
		
		outputFuture.thenApply((Map<String, String> output) -> {
			try {
				assertEquals("bar", output.get("foo"));
				testPromise.success(true);
			} catch(Throwable t) {
				testPromise.failure(t);
			}
			
			return null;
		});
	}
	
	@Test
	public void testSequence() {
		f.sequence(
			Arrays.asList(
				f.successful("Hello"),
				f.successful("world")))
				
				.thenAccept(list -> {
					try {
						assertEquals(list, Arrays.asList("Hello", "world"));
						
						testPromise.success(true);
					} catch(Throwable t) {
						testPromise.failure(t);
					}
				});
	}
	
	@Test
	public void testSequenceFailure() {
		f.sequence(
				Arrays.asList(
					f.successful("Hello"),
					f.failed(new IllegalStateException())))
					
				.exceptionally(e -> {
					try {
						assertTrue(e instanceof IllegalStateException);
						
						testPromise.success(true);
					} catch(Throwable t) {
						testPromise.failure(t);
					}
					
					return null;
				});
	}
	
	@Test
	public void testCast() {
		f.cast(f.successful(42), Number.class)
			.handle((n, throwable) -> {				
				try {
					assertNull(throwable);
					assertEquals(42, n);
					
					testPromise.success(true);
				} catch(Throwable t) {
					testPromise.failure(t);
				}
				
				return null;
		});
	}
	
	@Test
	public void testCastFailure() {
		f.cast(f.successful("Hello, world!"), Integer.class)
			.exceptionally(e -> {
				try {
					assertTrue(e instanceof WrongResultException);
					assertEquals("Hello, world!", ((WrongResultException)e).getResult());
					
					testPromise.success(true);
				} catch(Throwable t) {
					testPromise.failure(t);
				}
				
				return null;
			});
	}
}
