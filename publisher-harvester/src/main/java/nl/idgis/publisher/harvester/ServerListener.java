package nl.idgis.publisher.harvester;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageListener;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ServerListener extends MessageListener {
	
	public ServerListener(Config sslConfig, ActorRef connection) {
		super(true, sslConfig, connection);
	}

	public static Props props(Config sslConfig, ActorRef connection) {
		return Props.create(ServerListener.class, sslConfig, connection);
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
