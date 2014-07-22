package nl.idgis.publisher.harvester;

import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;

import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ServerActors extends MessageProtocolActors {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvester;
	
	public ServerActors(ActorRef harvester) {
		this.harvester = harvester;
	}

	public static Props props(ActorRef harvester) {
		return Props.create(ServerActors.class, harvester);
	}	

	@Override
	protected void createActors(ActorRef messagePackagerProvider) {
		log.debug("creating server actors");
		
		final ExecutionContextExecutor dispatcher = getContext().system().dispatcher();
		
		Future<Object> databasePackager = Patterns.ask(messagePackagerProvider, new GetMessagePackager("database"), 1000);
		final Future<Object> metadataPackager = Patterns.ask(messagePackagerProvider, new GetMessagePackager("metadata"), 1000);
		databasePackager.onSuccess(new OnSuccess<Object>() {

			@Override
			public void onSuccess(Object msg) {
				final ActorRef database = (ActorRef)msg;
				metadataPackager.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) {
						ActorRef metadata = (ActorRef)msg;
						getContext().actorOf(ProviderClient.props(harvester, metadata, database), "harvester");
					}
				}, dispatcher);
			}
		}, dispatcher);		
	}
}
