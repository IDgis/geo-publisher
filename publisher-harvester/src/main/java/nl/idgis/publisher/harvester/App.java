package nl.idgis.publisher.harvester;

import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.utils.Boot;
import nl.idgis.publisher.utils.ConfigUtils;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.typesafe.config.Config;

public class App extends UntypedActor {	
	
	private final Config config;
	private final LoggingAdapter log;

	public App(Config config) {
		this.config = config;
		
		log = Logging.getLogger(getContext().system(), this);
	}
	
	public static Props props(Config config) {
		return Props.create(App.class, config);
	}

	@Override
	public void preStart() {
		
		final int port = config.getInt("port");
		final Config sslConfig = ConfigUtils.getOptionalConfig(config, "ssl");		
		getContext().actorOf(Server.props(port, sslConfig), "server");
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
		Boot boot = Boot.init("harvester");
		boot.startApplication(App.props(boot.getConfig()));
	}
}
