package nl.idgis.publisher.harvester;

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
	
	public ServerListener(Config sslConfig) {
		this.sslConfig = sslConfig;
	}
	
	public static Props props(Config sslConfig) {
		return Props.create(ServerListener.class, sslConfig);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Connected) {
			log.debug("client connected");
			
			ActorRef actors = getContext().actorOf(ServerActors.props(), "serverActors");
			ActorRef messageProtocolHandler = getContext().actorOf(MessageProtocolHandler.props(true, sslConfig, getSender(), actors), "messages");			
			
			actors.tell(new ListenerInit(messageProtocolHandler), getSelf());
		}
	}

}
