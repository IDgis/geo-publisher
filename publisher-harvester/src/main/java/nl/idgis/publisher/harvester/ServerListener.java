package nl.idgis.publisher.harvester;

import nl.idgis.publisher.protocol.MessageListener;

import akka.actor.ActorRef;
import akka.actor.Props;

public class ServerListener extends MessageListener {
	
	public ServerListener(ActorRef connection) {
		super(true, connection);
	}

	public static Props props(ActorRef connection) {
		return Props.create(ServerListener.class, connection);
	}

	@Override
	protected void connected(LocalActorFactory actorFactory) {
		actorFactory
			.newActor("harvester")
			.actorOf(ProviderClient.props(
					actorFactory.getRemoteRef("metadata"), 
					actorFactory.getRemoteRef("database")));
	}

	@Override
	protected void connectionClosed() {

	}
}
