package nl.idgis.publisher.harvester;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageListener;
import akka.actor.Props;

public class ServerListener extends MessageListener {

	private final Config conf;

	public ServerListener(Config conf) {
		this.conf = conf;
	}

	public static Props props(Config conf) {
		return Props.create(ServerListener.class, conf);
	}

	@Override
	protected void connected(LocalActorFactory actorFactory) {
		actorFactory
			.newActor("harvester")
			.actorOf(ProviderClient.props(
					actorFactory.getRemoteRef("metadata"), 
					actorFactory.getRemoteRef("database"), 
					conf.getConfig("client")));
	}

	@Override
	protected void connectionClosed() {

	}
}
