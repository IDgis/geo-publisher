package nl.idgis.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.lang.reflect.Method;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedActor.MethodCall;
import akka.actor.TypedActor.Receiver;
import akka.actor.TypedActorExtension;
import akka.actor.TypedProps;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class TypedActorTest {
	
	public interface MyService {
		
		void doSomething();
		String getString();		
		Future<String> getStringAsync();
		Object getLastFuture();
	}
	
	public static class DefaultMyService implements Receiver, MyService {

		@Override
		public void doSomething() {
			
		}

		@Override
		public String getString() {
			return "string";
		}		
		
		Future<String> retval;
		
		private Future<String> getRealString() {
			return Futures.successful("asyncString");
		}
		
		private Future<Integer> anotherMethod() {
			return Futures.successful(42);
		}
		
		class IntToString extends Mapper<Integer, Future<String>> {
			
			@Override
			public Future<String> checkedApply(Integer i) throws Exception {
				if(i == 42) {
					return getRealString();
				}
				
				throw new IllegalArgumentException();
			}
		}

		@Override
		public Future<String> getStringAsync() {
			Future<Integer> firstFuture = anotherMethod();
			return firstFuture.flatMap(new IntToString(), TypedActor.context().dispatcher());			
		}
		
		public Object getLastFuture() {
			return retval;
		}

		@Override
		public void onReceive(Object msg, ActorRef sender) {
			sender.tell("response", ActorRef.noSender());
		}
		
	}

	@Test
	public void test() throws Exception {
		ActorSystem actorSystem = ActorSystem.create();
		TypedActorExtension typedActorExtension = TypedActor.get(actorSystem);
		
		TypedProps<DefaultMyService> props = new TypedProps<>(MyService.class, DefaultMyService.class);
		MyService myService = typedActorExtension.typedActorOf(props);
		assertEquals("string", myService.getString());
		
		Future<String> result = myService.getStringAsync();
		assertEquals("asyncString", Await.result(result, Timeout.apply(1000).duration()));
		
		@SuppressWarnings("unchecked")
		Future<String> lastFuture = (Future<String>)myService.getLastFuture();
		assertNotSame(result, lastFuture);
		
		ActorRef myServiceRef = typedActorExtension.getActorRefFor(myService);
		assertEquals("response", Await.result(Patterns.ask(myServiceRef, "Hello, world!", 1000), Timeout.apply(1000).duration()));
		
		Method m = MyService.class.getMethod("getString");
		assertEquals("string", Await.result(Patterns.ask(myServiceRef, new MethodCall(m, new Object[]{}), 1000), Timeout.apply(1000).duration())); 
	}
}
