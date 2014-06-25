package nl.idgis.publisher.harvester;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.ConnectionListener;
import akka.actor.Props;

public class ServerListener extends ConnectionListener {
	
	private final Config conf;
	
	public ServerListener(Config conf) {
		this.conf = conf;
	}
	
	public static Props props(Config conf) {
		return Props.create(ServerListener.class, conf);
	}	

	@Override
	protected void connected() {
		ActorBuilder builder = addActor("harvester");
		builder.actorOf(ProviderClient.props(builder.getRemoteRef("metadata"), builder.getRemoteRef("database"), conf.getConfig("client")));
	}

	@Override
	protected void connectionClosed() {	
		
	}
}
