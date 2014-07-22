package nl.idgis.publisher.harvester;

import nl.idgis.publisher.harvester.messages.ProviderConnected;
import nl.idgis.publisher.utils.ConfigUtils;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.typesafe.config.Config;

public class Harvester extends UntypedActor {
	
	private final Config config;
	private final ActorRef database;
	private final LoggingAdapter log;	
	
	private BiMap<String, ActorRef> providerClients;

	public Harvester(ActorRef database, Config config) {
		this.database = database;
		this.config = config;
		
		log = Logging.getLogger(getContext().system(), this);
	}
	
	public static Props props(ActorRef database, Config config) {
		return Props.create(Harvester.class, database, config);
	}

	@Override
	public void preStart() {
		final int port = config.getInt("port");
		final Config sslConfig = ConfigUtils.getOptionalConfig(config, "ssl");		
		getContext().actorOf(Server.props(getSelf(), port, sslConfig), "server");
		
		providerClients = HashBiMap.create();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ProviderConnected) {
			String providerName = ((ProviderConnected) msg).getName();
			log.debug("Provider connected: " + providerName);
			
			getContext().watch(getSender());
			providerClients.put(providerName, getSender());
		} else if (msg instanceof Terminated){
			String providerName = providerClients.inverse().remove(((Terminated) msg).getActor());
			if(providerName != null) {
				log.debug("Connection lost, provider: " + providerName);
			}
		} else {
			unhandled(msg);
		}
	}
}
