package nl.idgis.publisher.provider;

import java.io.File;

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
	protected void connected() {
		ActorBuilder metadataBuilder = addActor("metadata");		
		metadataBuilder.actorOf(Metadata.props(new File(".")));
		
		LocalActorRef providerRef = addActor("provider", app);
		providerRef.getRemoteRef("harvester").tell(new Hello("My data provider"), getSelf());		
	}

	@Override
	protected void connectionClosed() {
		app.tell(new ConnectionClosed(), getSelf());
	}
}
