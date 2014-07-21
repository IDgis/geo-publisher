package nl.idgis.publisher.service;

import scala.concurrent.Future;

import com.typesafe.config.Config;

import nl.idgis.publisher.database.PublisherDatabase;
import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.Version;
import nl.idgis.publisher.utils.Boot;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class App extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	public App(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(App.class, config);
	}
	
	@Override
	public void preStart() throws Exception {
		Config databaseConfig = config.getConfig("database");
		ActorRef database = getContext().actorOf(PublisherDatabase.props(databaseConfig), "database");
		Future<Object> version = Patterns.ask(database, new GetVersion(), 15000);
		version.onSuccess(new OnSuccess<Object>() {

			@Override
			public void onSuccess(Object msg) throws Throwable {
				Version version = (Version)msg;
				log.debug(version.toString());
			}
			
		}, getContext().dispatcher());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		
	}

	public static void main(String[] args) {
		Boot boot = Boot.init("service");
		boot.startApplication(App.props(boot.getConfig()));
	}
}
