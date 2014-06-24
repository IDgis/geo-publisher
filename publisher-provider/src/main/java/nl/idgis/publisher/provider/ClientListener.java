package nl.idgis.publisher.provider;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.protocol.ConnectionListener;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.provider.messages.ConnectionClosed;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ClientListener extends ConnectionListener {
	
	private ActorRef app;
	
	public ClientListener(ActorRef app) {
		this.app = app;
	}
	
	public static Props props(ActorRef app) {
		return Props.create(ClientListener.class, app);
	}

	@Override
	protected Map<String, ActorRef> connected() {
		ActorRef remoteHarvester = getRemoteRef("harvester");
		
		Map<String, ActorRef> localActors = new HashMap<String, ActorRef>();
		localActors.put("provider", app);
		localActors.put("metadata", getContext().actorOf(Metadata.props(new File("."), remoteHarvester)));
		
		remoteHarvester.tell(new Hello("My data provider"), getSelf());

		return localActors;
	}

	@Override
	protected void connectionClosed() {
		app.tell(new ConnectionClosed(), getSelf());
	}
}
