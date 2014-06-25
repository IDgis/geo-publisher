package nl.idgis.publisher.harvester;

import nl.idgis.publisher.protocol.ConnectionListener;

import akka.actor.Props;

public class ServerListener extends ConnectionListener {
	
	public ServerListener() {
		
	}
	
	public static Props props() {
		return Props.create(ServerListener.class);
	}	

	@Override
	protected void connected() {
		ActorBuilder builder = addActor("harvester");
		builder.actorOf(ProviderClient.props(builder.getRemoteRef("provider"), builder.getRemoteRef("metadata")));
	}

	@Override
	protected void connectionClosed() {	
		
	}
}
