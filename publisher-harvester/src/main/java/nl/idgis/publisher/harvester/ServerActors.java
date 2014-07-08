package nl.idgis.publisher.harvester;

import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import nl.idgis.publisher.protocol.GetMessagePackager;
import nl.idgis.publisher.protocol.ListenerInit;
import nl.idgis.publisher.utils.OnReceive;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class ServerActors extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public static Props props() {
		return Props.create(ServerActors.class);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ListenerInit) {
			ActorRef messagePackagerProvider = ((ListenerInit) msg).getMessagePackagerProvider();
			
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
		} else {
			unhandled(msg);
		}
	}
}
