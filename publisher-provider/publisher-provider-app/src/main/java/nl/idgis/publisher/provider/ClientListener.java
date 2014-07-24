package nl.idgis.publisher.provider;

import nl.idgis.publisher.protocol.MessageProtocolHandler;
import nl.idgis.publisher.protocol.messages.ListenerInit;
import nl.idgis.publisher.utils.ConfigUtils;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Connected;

public class ClientListener extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	public ClientListener(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(ClientListener.class, config);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Connected) {
			log.debug("connected");
			
			ActorRef actors = getContext().actorOf(ClientActors.props(config), "clientActors");
			
			Config sslConfig = ConfigUtils.getOptionalConfig(config, "ssl");
			ActorRef messageProtocolHandler = getContext().actorOf(MessageProtocolHandler.props(false, sslConfig, getSender(), actors), "messages");
			
			getContext().watch(messageProtocolHandler);
			
			actors.tell(new ListenerInit(messageProtocolHandler), getSelf());
		} else if(msg instanceof Terminated) {
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
}
