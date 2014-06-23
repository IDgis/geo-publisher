package nl.idgis.publisher.harvester;

import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.protocol.Message;
import nl.idgis.publisher.protocol.MessageDispatcher;
import nl.idgis.publisher.protocol.MessagePackager;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.io.Tcp.Connected;

public class ServerListener extends UntypedActor {
	
	private ActorRef dispatcher;
	
	public ServerListener() {
		
	}
	
	public static Props props() {
		return Props.create(ServerListener.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Connected) {
			ActorRef remoteProvider = getContext().actorOf(MessagePackager.props("provider", getSender()), "provider");
			ActorRef remoteMetadata = getContext().actorOf(MessagePackager.props("metadata", getSender()), "metadata");
			
			Map<String, ActorRef> localActors = new HashMap<String, ActorRef>();
			localActors.put("harvester", getContext().actorOf(ProviderClient.props(remoteProvider, remoteMetadata), "harvester")); 
			
			dispatcher = getContext().actorOf(MessageDispatcher.props(localActors));			
		} else if(msg instanceof Message) {
			if(dispatcher == null) {
				throw new IllegalStateException("Not connected");
			}
			
			dispatcher.tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
}
