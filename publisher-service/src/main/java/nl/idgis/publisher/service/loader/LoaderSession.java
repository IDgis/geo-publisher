package nl.idgis.publisher.service.loader;

import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.ImportLogLine;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class LoaderSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String datasetId;
	private final ActorRef database;
	
	private long count = 0;
	
	public LoaderSession(String datasetId, ActorRef database) {
		this.datasetId = datasetId;
		this.database = database;
	}
	
	public static Props props(String datasetId, ActorRef database) {
		return Props.create(LoaderSession.class, datasetId, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Record) {
			log.debug("record received: " + msg);
			
			count++;			
			getSender().tell(new NextItem(), getSelf());			
		} else if(msg instanceof Failure) {
			log.error("import failed: " + ((Failure) msg).getCause());
			
			getContext().stop(getSelf());
		} else if(msg instanceof End) {
			log.info("import completed");
			
			final ActorRef self = getSelf();
			ImportLogLine logLine = new ImportLogLine(GenericEvent.FINISHED, datasetId);
			Patterns.ask(database, new StoreLog(logLine), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						log.debug("import finished: " + count);
						
						getContext().stop(self);
					}					
				}, getContext().dispatcher());
		}
	}
}
