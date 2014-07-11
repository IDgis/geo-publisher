package nl.idgis.publisher.harvester;

import nl.idgis.publisher.protocol.GetMessagePackager;
import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.utils.OnReceive;

import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ServerActors extends MessageProtocolActors {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public static Props props() {
		return Props.create(ServerActors.class);
	}	

	@Override
	protected void createActors(ActorRef messagePackagerProvider) {
		log.debug("creating server actors");
		
		final ExecutionContextExecutor dispatcher = getContext().system().dispatcher();
		
		Future<Object> databasePackager = Patterns.ask(messagePackagerProvider, new GetMessagePackager("database"), 1000);
		final Future<Object> metadataPackager = Patterns.ask(messagePackagerProvider, new GetMessagePackager("metadata"), 1000);
		databasePackager.onComplete(new OnReceive<ActorRef>(log, ActorRef.class) {

			@Override
			protected void onReceive(final ActorRef database) {					
				metadataPackager.onComplete(new OnReceive<ActorRef>(log, ActorRef.class) {

					@Override
					protected void onReceive(ActorRef metadata) {
						getContext().actorOf(ProviderClient.props(metadata, database), "harvester");							
					}
				}, dispatcher);
			}
		}, dispatcher);		
	}
}
