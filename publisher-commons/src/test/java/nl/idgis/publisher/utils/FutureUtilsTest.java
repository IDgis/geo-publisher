package nl.idgis.publisher.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.Promise;
import scala.concurrent.duration.Duration;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import akka.dispatch.OnFailure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
			.collect(Futures.successful("Hello world!"))			
			.collect(Futures.successful(42))
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
			.failure(
				f
					.collect(Futures.successful("Hello world!"))			
					.collect(Futures.failed(new Exception("Failure")))
					.map((String s, Object o) -> {
						try {
							fail("result received");						
						} catch(Throwable t) {
							testPromise.failure(t);
						}
						
						return null;
					}),					
					
			new OnFailure() {

				@Override
				public void onFailure(Throwable t) throws Throwable {
					testPromise.success(true);
				}					
		});
	}
	
	@Test
	public void testCollectReturnValue() {
		f
			.collect(		
				f
					.collect(Futures.successful("Hello world!"))
					.collect(Futures.successful(42))
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
					.collect(Futures.successful("Hello world!"))
					.collect(Futures.successful(42))
					.flatMap((String s, Integer i) -> {
						return Futures.successful(47);						
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
	public void testCast() {
		
		Future<Object> objectFuture = Futures.<Object>successful("Hello world!");
		
		f
			.collect(f.cast(objectFuture, String.class))
			.map((String s) -> {
				testPromise.success(true);
				
				return null;
			});
	}
	
	@Test
	public void testMap() throws Exception {
		Map<String, Future<String>> input = new HashMap<String, Future<String>>();
		
		input.put("foo", Futures.successful("bar"));
		
		Future<Map<String, String>> outputFuture = f.map(input);
		assertNotNull(outputFuture);
		
		f
			.collect(outputFuture)
			.map((Map<String, String> output) -> {
				try {
					assertEquals("bar", output.get("foo"));
					testPromise.success(true);
				} catch(Throwable t) {
					testPromise.failure(t);
				}
				
				return null;
			});
	}
}
