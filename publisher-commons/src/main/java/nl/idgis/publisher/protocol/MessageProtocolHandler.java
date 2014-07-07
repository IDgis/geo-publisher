package nl.idgis.publisher.protocol;

import java.nio.ByteBuffer;

import nl.idgis.publisher.protocol.Message;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;
import akka.io.Tcp.Received;
import akka.io.TcpMessage;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.util.ByteString;

public class MessageProtocolHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Serialization serialization = SerializationExtension.get(getContext().system());
	
	private final boolean isServer;
	private final ActorRef connection, listener;
	
	private ByteString data = ByteString.empty();
	private ActorRef ssl;
	
	public MessageProtocolHandler(boolean isServer, ActorRef connection, ActorRef listener) {
		this.isServer = isServer;
		this.connection = connection;
		this.listener = listener;	
	}
	
	public static Props props(boolean isServer, ActorRef connection, ActorRef listener) {
		return Props.create(MessageProtocolHandler.class, isServer, connection, listener);
	}
	
	@Override
	public void preStart() throws Exception {
		ssl = getContext().actorOf(SSLHandler.props(isServer, connection, getSelf()), "ssl");
		
		connection.tell(TcpMessage.register(ssl), getSelf());
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
		} else if(msg instanceof Message) {
			final byte[] messageBytes = serialization.serialize(msg).get();
			
			ByteBuffer buffer = ByteBuffer.allocate(messageBytes.length + 4);
			buffer.putInt(messageBytes.length);
			buffer.put(messageBytes);
			buffer.flip();
						
			ByteString data = ByteString.fromByteBuffer(buffer);
			ssl.tell(TcpMessage.write(data), getSelf());
			
			log.debug("message sent: " + msg);
		} else {
			unhandled(msg);
		}
	}
}
