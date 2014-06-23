package nl.idgis.publisher.provider;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.Message;
import nl.idgis.publisher.protocol.MessageDispatcher;
import nl.idgis.publisher.protocol.MessagePackager;
import nl.idgis.publisher.provider.messages.CreateConnection;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.Connected;
import akka.io.Tcp.ConnectionClosed;

public class Provider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef client, dispatcher;
	
	@Override
	public void preStart() {		
		client = getContext().actorOf(Client.props(getSelf()), "client");		
		client.tell(new CreateConnection(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Connected) {
			log.debug("connected");
			
			ActorRef remoteHarvester = getContext().actorOf(MessagePackager.props("harvester", getSender()));
			
			Map<String, ActorRef> localActors = new HashMap<String, ActorRef>();
			localActors.put("provider", getSelf());
			localActors.put("metadata", getContext().actorOf(Metadata.props(new File("."), remoteHarvester)));
			
			dispatcher = getContext().actorOf(MessageDispatcher.props(localActors));
			
			remoteHarvester.tell(new Hello("My data provider"), getSelf());			
		} else if (msg instanceof Hello) {
			log.debug(msg.toString());
		} else if (msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			
			client.tell(new CreateConnection(), getSelf());
		} else if (msg instanceof Message) {
			if(dispatcher == null) {
				throw new IllegalStateException("Not connected");
			}
			
			dispatcher.tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
}