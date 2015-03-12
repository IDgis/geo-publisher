package nl.idgis.publisher.protocol;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Envelope;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.Message;
import nl.idgis.publisher.protocol.messages.StopPackager;
import nl.idgis.publisher.utils.FutureUtils;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

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
	
	static class TerminatedReceived {
		
		final ActorRef actor;
		
		TerminatedReceived(ActorRef actor) {
			this.actor = actor;
		}
		
		ActorRef actor() {
			return actor;
		}
	}
	
	static class Watcher extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		ActorRef sender;
		
		Terminated terminated;

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Terminated) {
				log.debug("terminated");
				
				terminated = (Terminated)msg;
				
				sendTerminated();
			} else if(msg instanceof GetTerminated) {
				log.debug("terminated requested");
				
				sender = getSender();
				
				sendTerminated();
			} else if(msg instanceof Watch) {
				log.debug("watch");
				
				getContext().watch(((Watch)msg).actor);
				
				getSender().tell(new Ack(), getSelf());
			} else {
				unhandled(msg);
			}
		}
		
		void sendTerminated() {
			if(sender != null && terminated != null) {
				sender.tell(new TerminatedReceived(terminated.actor()), getSelf());
				
				sender = null;
				terminated = null;
				
				log.debug("terminated sent");
			}
		}
		
	}
	
	static class GetMessage {
		
	}
	
	static class MessageTarget extends UntypedActor {
		
		final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		Message message;
		
		ActorRef sender;

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Message) {
				log.debug("message received");
				
				message = (Message)msg;
				
				sendMessage();
			} else if(msg instanceof GetMessage) {
				log.debug("message requested");
				
				sender = getSender();
				
				sendMessage();
			}
		}
		
		void sendMessage() {
			if(message != null && sender != null) {
				sender.tell(message, getSelf());
				
				log.debug("message sent");
			}
		}
		
	}
	
	ActorSystem system;

	ActorRef messagePackagerProvider, messageTarget, watcher;
	
	FutureUtils f;

	@Before
	public void actorSystem() {
		Config config = ConfigFactory.empty().withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("debug"));
		system = ActorSystem.create("test", config);
		
		watcher = system.actorOf(Props.create(Watcher.class), "watcher");
		messageTarget = system.actorOf(Props.create(MessageTarget.class), "message-target");
		messagePackagerProvider = system.actorOf(MessagePackagerProvider.props(messageTarget, "/path"), "message-packager-provider");
		
		f = new FutureUtils(system);
	}
	
	@After
	public void stop() {
		system.shutdown();
	}
	
	@Test
	public void testGetMessagePackager() throws Exception {
		f.ask(messagePackagerProvider, new GetMessagePackager("test"), ActorRef.class).get();		
	}
	
	@Test
	public void testStopPackager() throws Exception {
		ActorRef packager = f.ask(messagePackagerProvider, new GetMessagePackager("test", false), ActorRef.class).get();
		f.ask(watcher, new Watch(packager), Ack.class).get();
		f.ask(messagePackagerProvider, new StopPackager("test"), Ack.class).get();
		assertEquals(
				packager,
				f.ask(watcher, new GetTerminated(), TerminatedReceived.class).get().actor());
	}
	
	@Test
	public void testPersistentStopPackager() throws Exception {
		ActorRef packager = f.ask(messagePackagerProvider, new GetMessagePackager("test", true), ActorRef.class).get();		
		f.ask(messagePackagerProvider, new StopPackager("test"), Ack.class).get();
		
		packager.tell("Hello world!", ActorRef.noSender());
		
		Message message = f.ask(messageTarget, new GetMessage(), Message.class).get();		
		assertEquals("test", message.getTargetName());
		
		assertTrue(message instanceof Envelope);
		Envelope envelope = (Envelope)message;
		assertEquals("Hello world!", envelope.getContent());
	}
}
