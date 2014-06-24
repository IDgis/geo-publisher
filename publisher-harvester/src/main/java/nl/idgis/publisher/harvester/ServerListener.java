package nl.idgis.publisher.harvester;

import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.protocol.ConnectionListener;

import akka.actor.ActorRef;
import akka.actor.Props;

public class ServerListener extends ConnectionListener {
	
	public ServerListener() {
		
	}
	
	public static Props props() {
		return Props.create(ServerListener.class);
	}	

	@Override
	protected Map<String, ActorRef> connected() {
		ActorRef remoteProvider = getRemoteRef("provider");
		ActorRef remoteMetadata = getRemoteRef("metadata");
		
		Map<String, ActorRef> localActors = new HashMap<String, ActorRef>();
		localActors.put("harvester", getContext().actorOf(ProviderClient.props(remoteProvider, remoteMetadata), "harvester"));

		return localActors;
	}

	@Override
	protected void connectionClosed() {	
		
	}
}
