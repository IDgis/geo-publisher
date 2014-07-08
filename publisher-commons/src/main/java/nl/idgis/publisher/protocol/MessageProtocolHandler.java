package nl.idgis.publisher.protocol;

import java.nio.ByteBuffer;

import scala.concurrent.duration.Duration;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.Message;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategy.Directive;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;
import akka.io.Tcp.Received;
import akka.io.TcpMessage;
import akka.japi.Function;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.util.ByteString;

public class MessageProtocolHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Serialization serialization = SerializationExtension.get(getContext().system());
	
	private final boolean isServer;
	private final Config sslConfig;
	private final ActorRef connection, listener;
	
	private ByteString data = ByteString.empty();
	private ActorRef target;
	
	public MessageProtocolHandler(boolean isServer, Config sslConfig, ActorRef connection, ActorRef listener) {
		this.isServer = isServer;
		this.sslConfig = sslConfig;
		this.connection = connection;
		this.listener = listener;	
	}
	
	public static Props props(boolean isServer, Config sslConfig, ActorRef connection, ActorRef listener) {
		return Props.create(MessageProtocolHandler.class, isServer, sslConfig, connection, listener);
	}
	
	@Override
	public void preStart() throws Exception {
		if(sslConfig != null) {
			target = getContext().actorOf(SSLHandler.props(sslConfig, isServer, connection, getSelf()), "ssl");
			getContext().watch(target);
			connection.tell(TcpMessage.register(target), getSelf());
		} else {
			target = connection;
			connection.tell(TcpMessage.register(getSelf()), getSelf());
		}
	}

	@Override
	public void onReceive(final Object msg) throws Exception {
		if(msg instanceof Received) {
			data = data.concat(((Received)msg).data());
			
			while(data.size() > 4) {
				ByteString lengthField = data.take(4);
				int length = lengthField.toByteBuffer().asIntBuffer().get();
				
				log.debug("receiving message, length: " + length);
				
				if(data.size() >= length + 4) {
					data = data.drop(4);
					ByteString message = data.take(length);
					
					Message receivedMessage = serialization.deserialize(message.toArray(), Message.class).get();					
					log.debug("message received: "+ receivedMessage);
					
					listener.tell(receivedMessage, getSelf());
					
					data = data.drop(length);
				}
			}
		} else if(msg instanceof Close) {
			connection.tell(TcpMessage.close(), getSelf());
		} else if(msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			
			listener.tell(msg, getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Terminated) {
			if(((Terminated) msg).getActor().equals(target)) {
				log.debug("target actor terminated");
				getContext().stop(getSelf());
			} 
		} else if(msg instanceof Message) {
			final byte[] messageBytes = serialization.serialize(msg).get();
			
			ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length + 4);
			buffer.putInt(messageBytes.length);
			buffer.put(messageBytes);
			buffer.flip();
						
			ByteString data = ByteString.fromByteBuffer(buffer);
			target.tell(TcpMessage.write(data), getSelf());
			
			log.debug("message sent: " + msg);
		} else {
			unhandled(msg);
		}
	}
	
	private final static SupervisorStrategy strategy = 
		new OneForOneStrategy(-1, Duration.Inf(), new Function<Throwable, Directive>() {
			@Override
			public Directive apply(Throwable t) {
				return OneForOneStrategy.stop();
			}
		});

	@Override
	public SupervisorStrategy supervisorStrategy() { 
		return strategy;
	}
}
