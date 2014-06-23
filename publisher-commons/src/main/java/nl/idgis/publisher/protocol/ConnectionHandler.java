package nl.idgis.publisher.protocol;

import java.nio.ByteBuffer;
import java.util.Map;

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

public class ConnectionHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	private final Serialization serialization = SerializationExtension.get(getContext().system());
	
	private final ActorRef connection, listener;
	private final Map<String, ActorRef> targets;
	
	private ByteString data = ByteString.empty();
	
	public ConnectionHandler(ActorRef connection, ActorRef listener, Map<String, ActorRef> targets) {
		this.connection = connection;
		this.listener = listener;
		this.targets = targets;	
	}
	
	public static Props props(ActorRef connection, ActorRef listener, Map<String, ActorRef> targets) {
		return Props.create(ConnectionHandler.class, connection, listener, targets);
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
					
					final String targetName = receivedMessage.getTargetName();
					if(targets.containsKey(targetName)) {
						targets.get(targetName).tell(receivedMessage.getContent(), getSelf());
					} else {
						throw new IllegalArgumentException("Unknown target: " + targetName);
					}
					
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
			connection.tell(TcpMessage.write(data), getSelf());
			
			log.debug("message sent: " + msg);
		} else {
			unhandled(msg);
		}
	}
}
