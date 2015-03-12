package nl.idgis.publisher;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Procedure;
import akka.util.Timeout;

import static org.junit.Assert.assertEquals;

public class AbstractStateMachineTest {
	
	public static class DummyActor extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			
		}
		
	}
	
	public static class TestActor extends AbstractStateMachine<String> {
		
		private final ActorRef dummy;
		
		private ActorRef sender;
		
		public TestActor(ActorRef dummy) {
			super(Duration.create(1, TimeUnit.SECONDS));
			
			this.dummy = dummy;
		}

		@Override
		protected void timeout(String state) {
			sender.tell(state, getSelf());
			getContext().stop(getSelf());
		}
		
		private Procedure<Object> notExpectingAnswer() {
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					
				}
				
			};
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			sender = getSender();
			
			dummy.tell(msg, getSelf());
			
			become("asking dummy", notExpectingAnswer());
		}
		
	}
	
	ActorSystem system;
	
	ActorRef dummy, echo;
	
	FutureUtils f;
	
	@Before
	public void actorSystem() {
		system = ActorSystem.create();
		
		dummy = system.actorOf(Props.create(DummyActor.class));
		
		f = new FutureUtils(system, Timeout.apply(2, TimeUnit.SECONDS));
	}
	
	@After
	public void cleanup() {
		system.shutdown();
	}

	@Test
	public void testTimeout() throws Exception {
		ActorRef test = system.actorOf(Props.create(TestActor.class, dummy));
		assertEquals("asking dummy", f.ask(test, "test").get());
	}
}
