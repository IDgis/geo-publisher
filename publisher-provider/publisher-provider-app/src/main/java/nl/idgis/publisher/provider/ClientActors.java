package nl.idgis.publisher.provider;

import java.io.File;

import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.database.Database;
import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import com.typesafe.config.Config;

public class ClientActors extends MessageProtocolActors {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	public ClientActors(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(ClientActors.class, config);
	}
	
	protected void createActors(ActorRef messagePackagerProvider) {
		log.debug("creating client actors");
		
		Config databaseConfig = config.getConfig("database");

		String driver;
		if(databaseConfig.hasPath("driver")) {
			driver = databaseConfig.getString("driver");
		} else {
			driver = null;
		}
		
		getContext().actorOf(Database.props(
				driver,
				databaseConfig.getString("url"),
				databaseConfig.getString("user"),
				databaseConfig.getString("password")), "database");

		getContext().actorOf(Metadata.props(
				new File(config.getString("metadata.folder"))), "metadata");
		
		final ActorRef provider = getContext().actorOf(Provider.props());
								
		Future<Object> harvesterPackager = Patterns.ask(messagePackagerProvider, new GetMessagePackager("harvester"), 1000);
		harvesterPackager.onSuccess(new OnSuccess<Object>() {

			@Override
			public void onSuccess(Object msg) {
				ActorRef harvester = (ActorRef)msg;
				harvester.tell(new Hello("My data provider"), provider);
			}
		}, getContext().system().dispatcher());
	}
}
