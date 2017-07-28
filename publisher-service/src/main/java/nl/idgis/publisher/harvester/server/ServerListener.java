package nl.idgis.publisher.harvester.server;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageProtocolHandler;
import nl.idgis.publisher.protocol.messages.ListenerInit;
import nl.idgis.publisher.utils.ConfigUtils;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Connected;

public class ServerListener extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config harvesterConfig;	
	private final ActorRef harvester;
	private final String harvesterName;
	
	public ServerListener(String harvesterName, ActorRef harvester, Config harvesterConfig) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.harvesterConfig = harvesterConfig;
	}
	
	public static Props props(String harvesterName, ActorRef harvester, Config harvesterConfig) {
		return Props.create(ServerListener.class, harvesterName, harvester, harvesterConfig);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Connected) {
			log.debug("client connected");
			
			ActorRef actors = getContext().actorOf(ServerActors.props(harvesterName, harvester, harvesterConfig), "serverActors");
			
			Config sslConfig = ConfigUtils.getOptionalConfig(harvesterConfig, "ssl");
			ActorRef messageProtocolHandler = getContext().actorOf(MessageProtocolHandler.props(true, sslConfig, getSender(), actors), "messages");
			
			getContext().watch(messageProtocolHandler);
			
			actors.tell(new ListenerInit(messageProtocolHandler), getSelf());
		} else if(msg instanceof Terminated) {
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}

}
