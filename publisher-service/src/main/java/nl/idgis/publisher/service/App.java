package nl.idgis.publisher.service;

import scala.concurrent.Future;
import nl.idgis.publisher.database.PublisherDatabase;
import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.Version;
import nl.idgis.publisher.harvester.Harvester;
import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.service.admin.Admin;
import nl.idgis.publisher.service.load.Loader;
import nl.idgis.publisher.utils.Boot;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import com.typesafe.config.Config;

public class App extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	private ActorRef database, harvester, loader;
	
	public App(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(App.class, config);
	}
	
	@Override
	public void preStart() throws Exception {
		Config databaseConfig = config.getConfig("database");
		database = getContext().actorOf(PublisherDatabase.props(databaseConfig), "database");
		
		Future<Object> versionFuture = Patterns.ask(database, new GetVersion(), 15000);
		versionFuture.onSuccess(new OnSuccess<Object>() {

			@Override
			public void onSuccess(Object msg) throws Throwable {
				Version version = (Version)msg;
				log.debug("database version: " + version);
				
				Config harvesterConfig = config.getConfig("harvester");
				harvester = getContext().actorOf(Harvester.props(database, harvesterConfig), "harvester");
				
				loader = getContext().actorOf(Loader.props(database, harvester), "loader");
				
				getContext().actorOf(Admin.props(database, harvester), "admin");
			}
			
		}, getContext().dispatcher());
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Tree) {
			log.debug(msg.toString());
		} else {
			unhandled(msg);
		}
	}

	public static void main(String[] args) {
		Boot boot = Boot.init("service");
		boot.startApplication(App.props(boot.getConfig()));
	}
}
