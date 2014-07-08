package nl.idgis.publisher.provider;

import java.io.File;

import scala.concurrent.Future;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.GetMessagePackager;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.ListenerInit;
import nl.idgis.publisher.utils.OnReceive;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ClientActors extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	public ClientActors(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(ClientActors.class, config);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ListenerInit) {			
			ActorRef messagePackagerProvider = ((ListenerInit) msg).getMessagePackagerProvider();
			
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
			
			final ActorRef provider = null;
									
			Future<Object> harvesterPackager = Patterns.ask(messagePackagerProvider, new GetMessagePackager("harvester"), 1000);
			harvesterPackager.onComplete(new OnReceive<ActorRef>(log, ActorRef.class) {

				@Override
				protected void onReceive(ActorRef harvester) {
					harvester.tell(new Hello("My data provider"), provider);
				}
			}, getContext().system().dispatcher());
		} else {
			unhandled(msg);
		}
	}
}
