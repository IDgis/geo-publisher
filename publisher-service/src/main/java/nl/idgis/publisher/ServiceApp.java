package nl.idgis.publisher;

import java.util.concurrent.TimeUnit;

import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import nl.idgis.publisher.admin.Admin;
import nl.idgis.publisher.database.GeometryDatabase;
import nl.idgis.publisher.database.PublisherDatabase;
import nl.idgis.publisher.database.messages.GetVersion;
import nl.idgis.publisher.database.messages.Version;
import nl.idgis.publisher.harvester.Harvester;
import nl.idgis.publisher.job.Creator;
import nl.idgis.publisher.job.Initiator;
import nl.idgis.publisher.loader.Loader;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.utils.Boot;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import com.typesafe.config.Config;

public class ServiceApp extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	private ActorRef geometryDatabase, database, harvester, loader;
	
	public ServiceApp(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(ServiceApp.class, config);
	}
	
	@Override
	public void preStart() throws Exception {
		Config geometryDatabaseConfig = config.getConfig("geometry-database");
		geometryDatabase = getContext().actorOf(GeometryDatabase.props(geometryDatabaseConfig), "geometryDatabase");
		
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
				
				loader = getContext().actorOf(Loader.props(geometryDatabase, database, harvester), "loader");
				
				getContext().actorOf(Admin.props(database, harvester, loader), "admin");
				
				getContext().actorOf(Initiator.props(database, harvester, loader), "jobInitiator");
				
				getContext().actorOf(Creator.props(database), "jobCreator");
			}
			
		}, getContext().dispatcher());
		
		if(log.isDebugEnabled()) {
			ActorSystem system = getContext().system();
			system.scheduler().schedule(
					Duration.Zero(), 
					Duration.create(5, TimeUnit.SECONDS), 
					getSelf(), 
					new GetActiveJobs(), 
					getContext().dispatcher(), 
					getSelf());
		}
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Tree) {
			log.debug(msg.toString());
		} else if(msg instanceof GetActiveJobs) {
			Patterns.ask(loader, msg, 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						log.debug("active loader tasks: " + msg);
					}
					
				}, getContext().dispatcher());
			
			Patterns.ask(harvester, msg, 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("active harvester tasks: " + msg);
				}
				
			}, getContext().dispatcher());
		} else {
			unhandled(msg);
		}
	}

	public static void main(String[] args) {
		Boot boot = Boot.init("service");
		boot.startApplication(ServiceApp.props(boot.getConfig()));
	}
}
