package nl.idgis.publisher.protocol;

import java.net.URL;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorWithStash;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.japi.Procedure;
import akka.util.ByteString;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.recorder.AnyRecorder;
import nl.idgis.publisher.utils.FutureUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SSLHandlerTest {
	
	Config clientConfig, serverConfig;
	
	@Before
	public void config() {
		URL clientKeyStore = SSLHandlerTest.class.getResource("client.jks");
		assertNotNull(clientKeyStore);
		
		clientConfig = ConfigFactory.empty()
			.withValue("private.password", ConfigValueFactory.fromAnyRef("client"))
			.withValue("private.file", ConfigValueFactory.fromAnyRef(clientKeyStore.getFile()));
		
		URL serverKeyStore = SSLHandlerTest.class.getResource("server.jks");
		assertNotNull(serverKeyStore);
		
		serverConfig = ConfigFactory.empty()
			.withValue("private.password", ConfigValueFactory.fromAnyRef("server"))
			.withValue("private.file", ConfigValueFactory.fromAnyRef(serverKeyStore.getFile()));
	}
	
	ActorSystem actorSystem;
	
	FutureUtils f;
	
	@Before
	public void startActorSystem() {
		Config akkaConfig = ConfigFactory.empty()
				.withValue("akka.loggers", ConfigValueFactory.fromIterable(Arrays.asList("akka.event.slf4j.Slf4jLogger")))
				.withValue("akka.loglevel", ConfigValueFactory.fromAnyRef("DEBUG"));
			
		actorSystem = ActorSystem.create("test", akkaConfig);
		
		f = new FutureUtils(actorSystem);
	}
	
	@After
	public void stopActorSystem() {
		actorSystem.shutdown();
		actorSystem.awaitTermination();
	}
	
	static class Sync {
		
	}
	
	static class SynAck {
		
	}
	
	static class ConnectionMock extends UntypedActorWithStash {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		static Props props() {
			return Props.create(ConnectionMock.class);
		}
		
		public Procedure<Object> tranceiving(ActorRef peer, ActorRef handler) {
			log.debug("tranceiving: {} {}", peer, handler);
			
			unstashAll();
			
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					if(msg instanceof Tcp.Received) {
						log.debug("received: {}", getSelf());						
						
						handler.tell(msg, getSelf());
					} else if(msg instanceof Tcp.Write) {
						log.debug("write: {}", getSelf());
						
						Tcp.Write write = (Tcp.Write)msg;						
						getSender().tell(write.ack(), getSelf());
						peer.tell(new Tcp.Received(write.data()), getSelf());
					} else {
						unhandled(msg);
					}
				}
				
			};
		}
		
		public Procedure<Object> connected(ActorRef peer) {
			log.debug("connected: {}", peer);
			
			unstashAll();
			
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					if(msg instanceof Tcp.Register) {
						log.debug("register");
						
						getContext().become(tranceiving(peer, ((Tcp.Register) msg).handler()));						
					} else {
						stash();
					}
				}
				
			};
		}
		
		public Procedure<Object> sync() {
			return new Procedure<Object>() {

				@Override
				public void apply(Object msg) throws Exception {
					if(msg instanceof Ack) {
						log.debug("ack");
						getContext().become(connected(getSender()));
					} else {
						stash();
					}
				}
				
			};
		}

		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Sync) {
				log.debug("sync");
				getSender().tell(new SynAck(), getSelf());				
				getContext().become(sync());
			} else if(msg instanceof SynAck) {
				log.debug("sync-ack");
				getSender().tell(new Ack(), getSelf());
				getContext().become(connected(getSender()));
			} else {
				stash();
			}
		}		
	}
	
	static class GetReceived {
		
		private final int length;
		
		GetReceived(int length) {
			this.length = length;
		}
		
		int getLength() {
			return length;
		}
	}
	
	static class Receiver extends UntypedActorWithStash {
		
		private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		private ByteString result;
		
		static Props props() {
			return Props.create(Receiver.class);
		}
		
		@Override
		public void preStart() throws Exception {
			result = ByteString.empty();
		}
		
		@Override
		public void onReceive(Object msg) throws Exception {
			if(msg instanceof Tcp.Received) {
				log.debug("received");
				
				result = result.concat(((Tcp.Received)msg).data());
				unstashAll();
			} else if (msg instanceof GetReceived) {
				log.debug("get received");
				
				int length = ((GetReceived)msg).getLength();
				if(result.size() >= length) {
					log.debug("enough data received");					
					getSender().tell(result.take(length), getSelf());
					result = result.drop(length);					
				} else {
					log.debug("not enough data received");
					stash();
				}
			} else {
				unhandled(msg);
			}
		}
	}	
	
	@Test
	public void testTranceiving() throws Exception {
		ActorRef clientConnection = actorSystem.actorOf(ConnectionMock.props(), "client-connection");		
		ActorRef clientListener = actorSystem.actorOf(AnyRecorder.props(), "client-listener");		
		ActorRef clientHandler = actorSystem.actorOf(
			SSLHandler.props(clientConfig, false, clientConnection, clientListener),
			"client-handler");
		
		ActorRef serverConnection = actorSystem.actorOf(ConnectionMock.props(), "server-connection");		
		ActorRef serverListener = actorSystem.actorOf(Receiver.props(), "server-listener");		
		ActorRef serverHandler = actorSystem.actorOf(
			SSLHandler.props(serverConfig, true, serverConnection, serverListener),
			"server-handler");
		
		clientConnection.tell(TcpMessage.register(clientHandler), clientHandler);		
		serverConnection.tell(TcpMessage.register(serverHandler), serverHandler);
		
		serverConnection.tell(new Sync(), clientConnection);
		
		ByteString message = ByteString.empty();
		
		for(int i = 0; i < 1000; i++) {
			message = message.concat(ByteString.fromString("Hello world!"));
		}
		
		clientHandler.tell(TcpMessage.write(message), clientListener);
		
		ByteString received = f.ask(serverListener, new GetReceived(message.size()), ByteString.class).get();
		assertEquals(received, message);
	}
	
}
