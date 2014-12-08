package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.utils.Ask.Response;

import org.junit.Before;
import org.junit.Test;

import scala.concurrent.Await;
import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class AskTest {
	
	final Timeout timeout = new Timeout(1, TimeUnit.SECONDS);
	
	final Duration duration = timeout.duration();
	
	public static class Incrementer extends UntypedActor {

		@Override
		public void onReceive(Object msg) throws Exception {
			getSender().tell(((Integer)msg) + 1, getSelf());
		}
	}
	
	ActorSystem actorSystem;
	ActorRef incrementer;
	
	@Before
	public void actors() {
		actorSystem = ActorSystem.create();
		incrementer = actorSystem.actorOf(Props.create(Incrementer.class));
	}
	
	@Test
	public void testAsk() throws Exception {
		assertEquals(2, (int)Await.result(Patterns.ask(incrementer, 1, timeout), duration));
		
		assertEquals(3, (int)Await.result(Ask.ask(actorSystem, incrementer, 2, timeout), duration));
		
		Response response = Await.result(Ask.askResponse(actorSystem, incrementer, 3, timeout), duration);
		assertEquals(incrementer, response.getSender());
		assertEquals(4, (int)response.getMessage());
	}
}
