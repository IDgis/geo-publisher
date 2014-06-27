package nl.idgis.publisher.utils;

import java.io.File;

import nl.idgis.publisher.monitor.InactiveMonitor;
import nl.idgis.publisher.monitor.Monitor;
import nl.idgis.publisher.monitor.MonitorAspect;

import org.aspectj.lang.Aspects;

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
		Config defaultConf = ConfigFactory.load();
		
		Config conf;
		File confFile = new File(name + ".conf");
		if(confFile.exists()) {
			conf = ConfigFactory.parseFile(confFile).withFallback(defaultConf);
		} else {
			conf = defaultConf;
		}
		
		Config appConfig = conf.getConfig("publisher." + name);
		
		ActorSystem actorSystem = ActorSystem.create(name, appConfig);
		
		ActorRef monitor;
		if(Aspects.hasAspect(MonitorAspect.class)) {
			monitor = actorSystem.actorOf(Monitor.props(), "monitor");
			
			MonitorAspect monitorAspect = Aspects.aspectOf(MonitorAspect.class);
			monitorAspect.setMonitor(monitor);
		} else {
			monitor = actorSystem.actorOf(InactiveMonitor.props(), "monitor");
		}
		
		return new Boot(appConfig, actorSystem, monitor);
	} 
	
	public Config getConfig() {
		return config;
	}
	
	public ActorRef getMonitor() {
		return monitor;
	}
	
	public void startPublisher(Props props) {
		ActorRef app = actorSystem.actorOf(props, "app");
		actorSystem.actorOf(Terminator.props(app), "app-terminator");
	}
}
