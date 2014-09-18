package nl.idgis.publisher.protocol;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Envelope;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.Message;
import nl.idgis.publisher.protocol.messages.StopPackager;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;

import static nl.idgis.publisher.utils.TestPatterns.askAssert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessagePackagerProviderTest {
	
	static class Watch {
		ActorRef actor;
		
		Watch(ActorRef actor) {
			this.actor = actor;
		}
	}
	
	static class GetTerminated {
		
	}
	
	static class Watcher extends UntypedActor {
		
		ActorRef sender;
		Terminated terminated;

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Terminated) {
				terminated = (Terminated)msg;
				
				sendTerminated();
			} else if(msg instanceof GetTerminated) {
				sender = getSender();
				
				sendTerminated();
			} else if(msg instanceof Watch) {
				getContext().watch(((Watch)msg).actor);
				
				getSender().tell(new Ack(), getSelf());
			} else {
				unhandled(msg);
			}
		}
		
		void sendTerminated() {
			if(sender != null && terminated != null) {
				sender.tell(terminated, getSelf());
				
				sender = null;
				terminated = null;
			}
		}
		
	}
	
	static class GetMessage {
		
	}
	
	static class MessageTarget extends UntypedActor {
		
		Message message;
		ActorRef sender;

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Message) {
				message = (Message)msg;
				
				sendMessage();
			} else if(msg instanceof GetMessage) {
				sender = getSender();
				
				sendMessage();
			}
		}
		
		void sendMessage() {
			if(message != null && sender != null) {
				sender.tell(message, getSelf());
			}
		}
		
	}
	
	ActorSystem system;

	ActorRef messagePackagerProvider, messageTarget, watcher;

	@Before
	public void actors() {
		system = ActorSystem.create();
		
		watcher = system.actorOf(Props.create(Watcher.class));
		messageTarget = system.actorOf(Props.create(MessageTarget.class));
		messagePackagerProvider = system.actorOf(MessagePackagerProvider.props(messageTarget, "/path"));
	}
	
	@After
	public void stop() {
		system.shutdown();
	}
	
	@Test
	public void testGetMessagePackager() throws Exception {
		askAssert(messagePackagerProvider, new GetMessagePackager("test"), ActorRef.class);		
	}
	
	@Test
	public void testStopPackager() throws Exception {
		ActorRef packager = askAssert(messagePackagerProvider, new GetMessagePackager("test", false), ActorRef.class);
		askAssert(watcher, new Watch(packager), Ack.class);
		askAssert(messagePackagerProvider, new StopPackager("test"), Ack.class);
		assertEquals(
				packager,
				askAssert(watcher, new GetTerminated(), Terminated.class).actor());
	}
	
	@Test
	public void testPersistentStopPackager() throws Exception {
		ActorRef packager = askAssert(messagePackagerProvider, new GetMessagePackager("test", true), ActorRef.class);		
		askAssert(messagePackagerProvider, new StopPackager("test"), Ack.class);		
		
		packager.tell("Hello world!", ActorRef.noSender());
		
		Message message = askAssert(messageTarget, new GetMessage(), Message.class);		
		assertEquals("test", message.getTargetName());
		
		assertTrue(message instanceof Envelope);
		Envelope envelope = (Envelope)message;
		assertEquals("Hello world!", envelope.getContent());
	}
}
