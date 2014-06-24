package nl.idgis.publisher.protocol;

import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Connected;
import akka.io.Tcp.ConnectionClosed;

public abstract class ConnectionListener extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef dispatcher;
	
	protected ActorRef connection;	
	
	protected abstract Map<String, ActorRef> connected();
	
	protected abstract void connectionClosed();
	
	protected ActorRef getRemoteRef(String targetName) {
		if(connection == null) {
			throw new IllegalStateException("Not connected");
		}
		
		return getContext().actorOf(MessagePackager.props(targetName, connection));
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Connected) {
			log.debug("connected");
			
			connection = getSender();
			dispatcher = getContext().actorOf(MessageDispatcher.props(connected()));		
		} else if (msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			connectionClosed();
			getContext().stop(getSelf());
		} else if (msg instanceof Message) {
			if(dispatcher == null) {
				throw new IllegalStateException("No dispatcher");
			}
			
			dispatcher.tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
}
