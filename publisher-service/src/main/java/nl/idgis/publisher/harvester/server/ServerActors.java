package nl.idgis.publisher.harvester.server;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.sources.ProviderConnectionClient;
import nl.idgis.publisher.protocol.MessageProtocolActors;

public class ServerActors extends MessageProtocolActors {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	private final ActorRef harvester;
	
	public ServerActors(String harvesterName, ActorRef harvester) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
	}

	public static Props props(String harvesterName, ActorRef harvester) {
		return Props.create(ServerActors.class, harvesterName, harvester);
	}	

	@Override
	protected void createActors(ActorRef messagePackagerProvider) {
		log.debug("creating server actors");
		
		getContext().actorOf(ProviderConnectionClient.props(harvesterName, harvester), "harvester");				
	}
}
