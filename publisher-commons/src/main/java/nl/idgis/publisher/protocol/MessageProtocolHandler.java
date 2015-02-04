package nl.idgis.publisher.protocol;

import java.nio.ByteBuffer;

import scala.concurrent.duration.Duration;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.messages.Close;
import nl.idgis.publisher.protocol.messages.GetTransferedTotal;
import nl.idgis.publisher.protocol.messages.Message;
import nl.idgis.publisher.protocol.messages.Register;
import nl.idgis.publisher.protocol.messages.Registered;
import nl.idgis.publisher.protocol.messages.TransferedTotal;

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
	
	private final ActorRef connection, container;	
	
	private ByteString data = ByteString.empty();
	
	private ActorRef messageTarget, messagePackagerProvider, messageDispatcher;
	
	private long received = 0, sent = 0;
	
	public MessageProtocolHandler(boolean isServer, Config sslConfig, ActorRef connection, ActorRef container) {
		this.isServer = isServer;
		this.sslConfig = sslConfig;
		this.connection = connection;
		this.container = container;
	}
	
	public static Props props(boolean isServer, Config sslConfig, ActorRef connection, ActorRef container) {
		return Props.create(MessageProtocolHandler.class, isServer, sslConfig, connection, container);
	}
	
	@Override
	public void preStart() throws Exception {
		if(sslConfig != null) {
			messageTarget = getContext().actorOf(SSLHandler.props(sslConfig, isServer, connection, getSelf()), "ssl");
			getContext().watch(messageTarget);
			connection.tell(TcpMessage.register(messageTarget), getSelf());
		} else {
			messageTarget = connection;
			connection.tell(TcpMessage.register(getSelf()), getSelf());
		}		
	}

	@Override
	public void onReceive(final Object msg) throws Exception {
		if(msg instanceof Received) {
			ByteString newData = ((Received)msg).data();
			int newDataSize = newData.size();
			
			log.debug("new data received: {}", newDataSize);
			
			received += newDataSize;
			
			data = data.concat(newData);
			
			while(data.size() > 4) {
				ByteString lengthField = data.take(4);
				int length = lengthField.toByteBuffer().asIntBuffer().get();
				
				log.debug("receiving message, length: " + length);
				
				int requiredSize = length + 4;
				if(data.size() >= requiredSize) {
					data = data.drop(4);
					ByteString message = data.take(length);
					
					Message receivedMessage = serialization.deserialize(message.toArray(), Message.class).get();					
					log.debug("message received: "+ receivedMessage);
					
					messageDispatcher.tell(receivedMessage, getSelf());
					
					data = data.drop(length);
				} else {
					log.debug("not enough bytes received yet: " + data.size() + "/" + requiredSize);
					break;
				}
			}
		} else if(msg instanceof Close) {
			connection.tell(TcpMessage.close(), getSelf());
		} else if(msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			getContext().stop(getSelf());
		} else if(msg instanceof Terminated) {
			if(((Terminated) msg).getActor().equals(messageTarget)) {
				log.debug("target actor terminated");
				getContext().stop(getSelf());
			} 
		} else if(msg instanceof Message) {
			final byte[] messageBytes = serialization.serialize(msg).get();
			
			sent += messageBytes.length;
			
			ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length + 4);
			buffer.putInt(messageBytes.length);
			buffer.put(messageBytes);
			buffer.flip();
						
			ByteString data = ByteString.fromByteBuffer(buffer);
			messageTarget.tell(TcpMessage.write(data), getSelf());
			
			log.debug("message sent: " + msg);
		} else if (msg instanceof Register) {
			log.debug("actors registered");
			
			ActorRef messageProtocolActors = ((Register) msg).getMessageProtocolActors();
			
			messagePackagerProvider = getContext().actorOf(MessagePackagerProvider.props(getSelf(), messageProtocolActors.path().toString()), "packagerProvider");
			messageDispatcher = getContext().actorOf(MessageDispatcher.props(messagePackagerProvider, container), "dispatcher");
			
			getSender().tell(new Registered(messagePackagerProvider), getSender());
		} else if(msg instanceof GetTransferedTotal) {
			getSender().tell(new TransferedTotal(received, sent), getSelf());
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
