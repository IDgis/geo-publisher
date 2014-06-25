package nl.idgis.publisher.protocol;

import java.util.HashMap;
import java.util.Map;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Connected;
import akka.io.Tcp.ConnectionClosed;

public abstract class ConnectionListener extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);

	private ActorRef dispatcher;

	protected ActorRef connection;

	private Map<String, ActorRef> localActors = new HashMap<String, ActorRef>();
	
	private Map<ActorPair, ActorRef> remoteRefs = new HashMap<ActorPair, ActorRef>();

	protected abstract void connected();

	protected abstract void connectionClosed();
	
	protected static class ActorPair {
		
		private final String targetName, sourceName;
		
		ActorPair(String targetName, String sourceName) {
			this.targetName = targetName;
			this.sourceName = sourceName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((sourceName == null) ? 0 : sourceName.hashCode());
			result = prime * result
					+ ((targetName == null) ? 0 : targetName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ActorPair other = (ActorPair) obj;
			if (sourceName == null) {
				if (other.sourceName != null)
					return false;
			} else if (!sourceName.equals(other.sourceName))
				return false;
			if (targetName == null) {
				if (other.targetName != null)
					return false;
			} else if (!targetName.equals(other.targetName))
				return false;
			return true;
		}
	}

	protected class LocalActorRef {

		protected final String name;

		private LocalActorRef(String name) {
			this.name = name;
		}

		public ActorRef getRemoteRef(String targetName) {
			ActorRef remoteRef = getContext().actorOf(
					MessagePackager.props(targetName, name, connection));
			
			remoteRefs.put(new ActorPair(name, targetName), remoteRef);
			
			return remoteRef;
		}
	}

	protected class ActorBuilder extends LocalActorRef {

		private ActorBuilder(String name) {
			super(name);
		}

		public void actorOf(Props props) {
			localActors.put(name, getContext().actorOf(props, name));
		}
	}

	protected LocalActorRef addActor(String name, ActorRef actorRef) {
		localActors.put(name, actorRef);
		return new LocalActorRef(name);
	}

	protected ActorBuilder addActor(String name) {
		return new ActorBuilder(name);
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof Connected) {
			if (dispatcher != null) {
				throw new IllegalStateException("Dispatcher already created");
			}

			log.debug("connected");

			connection = getSender();
			connected();
			dispatcher = getContext().actorOf(
					MessageDispatcher.props(localActors, remoteRefs));
		} else if (msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			connectionClosed();
			getContext().stop(getSelf());
		} else if (msg instanceof Message) {
			if (dispatcher == null) {
				throw new IllegalStateException("No dispatcher");
			}

			dispatcher.tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
}
