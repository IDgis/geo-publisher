package nl.idgis.publisher.harvester.server;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageProtocolHandler;
import nl.idgis.publisher.protocol.messages.ListenerInit;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Connected;

public class ServerListener extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config sslConfig;	
	private final ActorRef harvester;
	private final String harvesterName;
	
	public ServerListener(String harvesterName, ActorRef harvester, Config sslConfig) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.sslConfig = sslConfig;
	}
	
	public static Props props(String harvesterName, ActorRef harvester, Config sslConfig) {
		return Props.create(ServerListener.class, harvesterName, harvester, sslConfig);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Connected) {
			log.debug("client connected");
			
			ActorRef actors = getContext().actorOf(ServerActors.props(harvesterName, harvester), "serverActors");
			ActorRef messageProtocolHandler = getContext().actorOf(MessageProtocolHandler.props(true, sslConfig, getSender(), actors), "messages");			
			
			actors.tell(new ListenerInit(messageProtocolHandler), getSelf());
		}
	}

}
