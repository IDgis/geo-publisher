package nl.idgis.publisher.provider;

import java.io.File;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageListener;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ClientListener extends MessageListener {

	private ActorRef app, monitor;
	private Config config;

	public ClientListener(Config config, ActorRef app, ActorRef monitor) {
		this.config = config;
		this.app = app;
		this.monitor = monitor;
	}

	public static Props props(Config config, ActorRef app, ActorRef monitor) {
		return Props.create(ClientListener.class, config, app, monitor);
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
					
			.existingActor("monitor", monitor)
			.existingActor("provider", app)
			
			.getRemoteRef("harvester").tell(
				new Hello("My data provider"), getSelf());
	}

	@Override
	protected void connectionClosed() {
		app.tell(new ConnectionClosed(), getSelf());
	}
}
