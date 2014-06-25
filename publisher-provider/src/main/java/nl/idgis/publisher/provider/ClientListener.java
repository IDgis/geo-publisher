package nl.idgis.publisher.provider;

import java.io.File;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.ConnectionListener;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ClientListener extends ConnectionListener {

	private ActorRef app;
	private Config config;

	public ClientListener(Config config, ActorRef app) {
		this.config = config;
		this.app = app;
	}

	public static Props props(Config config, ActorRef app) {
		return Props.create(ClientListener.class, config, app);
	}

	@Override
	protected void connected() {
		ActorBuilder metadataBuilder = addActor("metadata");
		metadataBuilder.actorOf(Metadata.props(new File(config.getString("metadata.folder"))));

		Config database = config.getConfig("database");
		ActorBuilder databaseBuilder = addActor("database");
		databaseBuilder.actorOf(Database.props(database.getString("url"),
				database.getString("user"), database.getString("password")));

		LocalActorRef providerRef = addActor("provider", app);
		providerRef.getRemoteRef("harvester").tell(
				new Hello("My data provider"), getSelf());
	}

	@Override
	protected void connectionClosed() {
		app.tell(new ConnectionClosed(), getSelf());
	}
}
