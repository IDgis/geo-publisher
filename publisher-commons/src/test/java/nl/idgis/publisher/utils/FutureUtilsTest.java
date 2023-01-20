package nl.idgis.publisher.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import akka.actor.ActorSystem;
import akka.actor.UntypedActor;
import akka.actor.Props;
import akka.actor.ActorRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.instanceOf;

public class FutureUtilsTest {
	
	private FutureUtils f;
	
	private CompletableFuture<Object> testFuture;

	private ActorRef echoActorRef;

	static class EchoActor extends UntypedActor {

		static Props props() {
			return Props.create(EchoActor.class);
		}

		@Override
		public final void onReceive(final Object msg) throws Exception {
			getSender().tell(msg, getSelf());
		}
	}
	
	@Before
	public void setUp() {
		
		ActorSystem system = ActorSystem.create();
		echoActorRef = system.actorOf(EchoActor.props());
		f = new FutureUtils(system);
		
		testFuture = new CompletableFuture<>();
	}
	
	@After
	public void doAssert() throws Throwable {
		try {
			testFuture.get(1, TimeUnit.SECONDS);
		} catch(ExecutionException e) {
			throw e.getCause();
		}
	}

	@Test
	public void testCollector() throws Throwable {
		
		f
			.collect(f.successful("Hello world!"))			
			.collect(f.successful(42))
			.thenApply((String s, Integer i) -> {
				try {
					assertEquals("Hello world!", s);
					assertEquals(new Integer(42), i);
					testFuture.complete(true);
				} catch(Throwable t) {
					testFuture.completeExceptionally(t);
				}
				
				return null;
			});
	}
	
	@Test
	public void testCollectFailure() throws Throwable {
		f
			.collect(f.successful("Hello world!"))			
			.collect(f.failed(new Exception("Failure")))
			.thenApply((String s, Object o) -> {
				try {
					fail("result received");						
				} catch(Throwable t) {
					testFuture.completeExceptionally(t);
				}
				
				return null;
			}).exceptionally(t -> testFuture.complete(true));		
	}
	
	@Test
	public void testCollectReturnValue() {
		f
			.collect(		
				f
					.collect(f.successful("Hello world!"))
					.collect(f.successful(42))
					.thenApply((String s, Integer i) -> {						
						return 47;
					}))
			.thenApply((Integer i) -> {
				try {
					assertEquals(new Integer(47), i);
					testFuture.complete(true);
				} catch(Throwable t) {
					testFuture.completeExceptionally(t);
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
					.thenCompose((String s, Integer i) -> {
						return f.successful(47);						
					}))
			.thenApply((Integer i) -> {
				try {
					assertEquals(new Integer(47), i);
					testFuture.complete(true);
				} catch(Throwable t) {
					testFuture.completeExceptionally(t);
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
				testFuture.complete(true);
			} catch(Throwable t) {
				testFuture.completeExceptionally(t);
			}
			
			return null;
		});
	}
	
	@Test
	public void testSupplierSequence() {
		f.<String>supplierSequence(
			Arrays.asList(
				() -> f.successful("Hello"),
				() -> f.successful("world")))
				
				.thenAccept(list -> {
					try {
						assertEquals(list, Arrays.asList("Hello", "world"));
						
						testFuture.complete(true);
					} catch(Throwable t) {
						testFuture.completeExceptionally(t);
					}
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
						
						testFuture.complete(true);
					} catch(Throwable t) {
						testFuture.completeExceptionally(t);
					}
				});
	}
	
	@Test
	public void testCollect() {
		Arrays.asList(
			f.successful("Hello"), 
			f.successful("world")).stream()
				.collect(f.collect()).thenAccept(stream -> {
					try {
						assertEquals(
							stream.collect(Collectors.toList()), 
							Arrays.asList("Hello", "world"));
						
						testFuture.complete(true);
					} catch(Throwable t) {
						testFuture.completeExceptionally(t);
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
						
						testFuture.complete(true);
					} catch(Throwable t) {
						testFuture.completeExceptionally(t);
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
					
					testFuture.complete(true);
				} catch(Throwable t) {
					testFuture.completeExceptionally(t);
				}
				
				return null;
		});
	}
	
	@Test
	public void testCastFailure() {
		f.cast(f.successful("Hello, world!"), Integer.class)
			.exceptionally(e -> {
				try {
					assertThat(e, instanceOf(CompletionException.class));
					
					Throwable cause = e.getCause();
					assertThat(cause, instanceOf(WrongResultException.class));
					
					WrongResultException wrongResultException = (WrongResultException)cause;
					assertEquals("Hello, world!", wrongResultException.getResult());
					assertEquals(Integer.class, wrongResultException.getExpected());
					
					testFuture.complete(true);
				} catch(Throwable t) {
					testFuture.completeExceptionally(t);
				}
				
				return null;
			});
	}
	
	@Test
	public void testConcat() {
		f.concat(
			f.successful(Stream.of("a", "b")),
			f.successful(Stream.of("c"))).handle((stream, throwable) -> {
				try {
					Set<String> result = stream.collect(Collectors.toSet());
					
					assertTrue(result.contains("a"));
					assertTrue(result.contains("b"));
					assertTrue(result.contains("c"));
					
					testFuture.complete(true);
				} catch(Throwable t) {
					testFuture.completeExceptionally(t);
				}
				
				return null;
			});
	}

	@Test
	public void testAsk() {
		f.ask(echoActorRef, "Hello world!").handle((echo, throwable) -> {
			try {
				assertEquals("Hello world!", echo);

				testFuture.complete(true);
			} catch(Throwable t) {
				testFuture.completeExceptionally(t);
			}

			return null;
		});
	}
}
