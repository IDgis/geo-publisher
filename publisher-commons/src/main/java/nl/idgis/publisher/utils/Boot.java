package nl.idgis.publisher.utils;

import java.io.File;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.monitor.Monitor;
import nl.idgis.publisher.monitor.MonitorAspect;
import nl.idgis.publisher.monitor.messages.GetStatus;

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
	
	public static void startPublisher(String name, Class<?> clazz) {
		
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
		
		ActorRef app = actorSystem.actorOf(Props.create(clazz, appConfig), "app");
		actorSystem.actorOf(Terminator.props(app), "app-terminator");
		
		if(Aspects.hasAspect(MonitorAspect.class)) {
			ActorRef monitor = actorSystem.actorOf(Monitor.props());
			
			MonitorAspect monitorAspect = Aspects.aspectOf(MonitorAspect.class);
			monitorAspect.setMonitor(monitor);
			
			actorSystem.scheduler().schedule(Duration.Zero(), Duration.create(10, TimeUnit.SECONDS), 
					monitor, new GetStatus(), actorSystem.dispatcher(), ActorRef.noSender());
		}
	}
}
