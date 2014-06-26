package nl.idgis.publisher.provider;

import java.io.File;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageListener;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ClientListener extends MessageListener {

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
	protected void connected(LocalActorFactory actorFactory) {
		Config database = config.getConfig("database");

		String driver;
		if(database.hasPath("driver")) {
			driver = database.getString("driver");
		} else {
			driver = null;
		}
		
		actorFactory
			.newActor("metadata")
			.actorOf(Metadata.props(
					new File(config.getString("metadata.folder"))))
			
			.newActor("database")
			.actorOf(Database.props(
					driver, 
					database.getString("url"),
					database.getString("user"), 
					database.getString("password")))
					
			.existingActor("provider", app);

		actorFactory.getRemoteRef("harvester").tell(
				new Hello("My data provider"), getSelf());
	}

	@Override
	protected void connectionClosed() {
		app.tell(new ConnectionClosed(), getSelf());
	}
}
