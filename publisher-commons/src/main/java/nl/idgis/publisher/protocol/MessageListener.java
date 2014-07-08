package nl.idgis.publisher.protocol;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import akka.io.Tcp.ConnectionClosed;

public abstract class MessageListener extends UntypedActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().system(),
			this);
	
	protected class LocalActorFactory {

		String name;

		private LocalActorFactory() {

		}

		public LocalActorFactory newActor(String name) {
			this.name = name;

			return this;
		}

		public LocalActorFactory existingActor(String name, ActorRef actorRef) {
			log.debug("existing actor added: " + name);

			this.name = name;

			localActors.put(name, new LocalActor(actorRef));

			return this;
		}

		public ActorRef getRemoteRef(String targetName) {
			if(name == null) {
				throw new IllegalStateException("No actor");
			}
			
			log.debug("creating remote ref: " + name + " -> " + targetName);			

			Props remoteRefProps = MessagePackager.props(targetName, name, messageHandler);
			String actorName = "remote"
					+ StringUtils.capitalize(name)
					+ StringUtils.capitalize(targetName);
			return getContext().actorOf(remoteRefProps, actorName);
		}

		public LocalActorFactory actorOf(Props props) {
			if(name == null) {
				throw new IllegalStateException("No actor");
			}
			
			log.debug("new actor added: " + name);
			
			localActors.put(name, new LocalActor(getContext().actorOf(props, name)));
			
			return this;
		}
	}

	private class LocalActor {

		final ActorRef actorRef;
		final Map<String, ActorRef> packagers;

		LocalActor(ActorRef actorRef) {
			this.actorRef = actorRef;
			this.packagers = new HashMap<String, ActorRef>();
		}
	}

	private Map<String, LocalActor> localActors = new HashMap<String, LocalActor>();

	protected abstract void connected(LocalActorFactory actorFactory);

	protected abstract void connectionClosed();
	
	private final boolean isServer;
	private final ActorRef connection;
	
	private ActorRef messageHandler;
	
	protected MessageListener(boolean isServer, ActorRef connection) {
		this.isServer = isServer;
		this.connection = connection;
	}
	
	@Override
	public final void preStart() {
		messageHandler = getContext().actorOf(MessageProtocolHandler.props(isServer, connection, getSelf()), "messages");
		
		connected(new LocalActorFactory());
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if (msg instanceof ConnectionClosed) {
			log.debug("disconnected");

			connectionClosed();
			getContext().stop(getSelf());
		} else if (msg instanceof Message) {
			log.debug("dispatching message");

			Message message = (Message) msg;

			String targetName = message.getTargetName();
			LocalActor localActor = localActors.get(targetName);

			if (localActor == null) {
				throw new IllegalStateException("Local actor unknown: "
						+ targetName);
			}

			ActorRef returnPackager;
			String sourceName = message.getSourceName();
			if (localActor.packagers.containsKey(sourceName)) {
				returnPackager = localActor.packagers.get(sourceName);
			} else {
				log.debug("creating remote ref: " + targetName + " -> " + sourceName);
				
				Props returnProps = MessagePackager.props(sourceName, targetName, getSender());
				String actorName = "return"
						+ StringUtils.capitalize(targetName)
						+ StringUtils.capitalize(sourceName);						
				returnPackager = getContext().actorOf(returnProps, actorName);
				localActor.packagers.put(sourceName, returnPackager);
			}

			localActor.actorRef.tell(message.getContent(), returnPackager);
		} else {
			unhandled(msg);
		}
	}
}
