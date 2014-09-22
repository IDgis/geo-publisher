package nl.idgis.publisher.utils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.monitor.Monitor;
import nl.idgis.publisher.monitor.MonitorAspect;
import nl.idgis.publisher.monitor.messages.GetTree;

import org.aspectj.lang.Aspects;

import scala.concurrent.duration.Duration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Boot {
	
	public static class Terminator extends UntypedActor {
		
		private final ActorRef app;
		private final LoggingAdapter log;		
		
		public Terminator(ActorRef app) {
			this.app = app;
			
			log = Logging.getLogger(getContext().system(), this);
		}
		
		@Override
		public void preStart() {
			getContext().watch(app);
			
			log.debug("watching application");
		}
		
		public static Props props(ActorRef app) {
			return Props.create(Terminator.class, app);
		}

		@Override
		public void onReceive(Object msg) throws Exception {			
			if(msg instanceof Terminated) {
				log.debug("terminating actor system");
				getContext().system().shutdown();
			} else {
				unhandled(msg);
			}
		}
	}
	
	private final Config config;
	private final ActorSystem actorSystem;
	private final ActorRef monitor;		
	
	public Boot(Config config, ActorSystem actorSystem, ActorRef monitor) {		
		this.config = config;
		this.actorSystem = actorSystem;
		this.monitor = monitor;		
	}
	
	public static Boot init(String name) {
		final Config defaultConf = ConfigFactory.load();
		
		final Config conf;
		final File confFile = new File(name + ".conf");
		if(confFile.exists()) {
			conf = ConfigFactory.parseFile(confFile).withFallback(defaultConf);
		} else {
			conf = defaultConf;
		}
		
		final Config appConfig = conf.getConfig("publisher." + name);
		
		final ActorSystem actorSystem = ActorSystem.create(name, appConfig);
		
		final ActorRef monitor;
		if(Aspects.hasAspect(MonitorAspect.class)) {
			monitor = actorSystem.actorOf(Monitor.props(), "monitor");
			
			MonitorAspect monitorAspect = Aspects.aspectOf(MonitorAspect.class);
			monitorAspect.setMonitor(monitor);
		} else {
			monitor = null;
		}
		
		return new Boot(appConfig, actorSystem, monitor);
	} 
	
	public Config getConfig() {
		return config;
	}
	
	public ActorRef getMonitor() {
		return monitor;
	}
	
	public ActorRef startApplication(Props props) {
		final ActorRef app = actorSystem.actorOf(props, "app");
		
		if(monitor != null) {
			Config monitorConfig = config.getConfig("monitor");
			if(monitorConfig.getBoolean("showTrees")) {		
				actorSystem.scheduler().schedule(Duration.Zero(), Duration.create(10, TimeUnit.SECONDS), 
					monitor, new GetTree(), actorSystem.dispatcher(), app);
			}
		}
		
		actorSystem.actorOf(Terminator.props(app), "app-terminator");
		
		return app;
	}
	
	public void awaitTermination() {
		actorSystem.awaitTermination();
	}
}
