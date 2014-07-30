package nl.idgis.publisher.harvester;

import nl.idgis.publisher.database.messages.RegisterSourceDataset;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.HarvestLogLine;
import nl.idgis.publisher.domain.service.Dataset;
import nl.idgis.publisher.harvester.sources.messages.Finished;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class HarvestSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final String dataSourceId;
	
	public HarvestSession(ActorRef database, String dataSourceId) {
		this.database = database;
		this.dataSourceId = dataSourceId;
	}
	
	public static Props props(ActorRef database, String dataSourceId) {
		return Props.create(HarvestSession.class, database, dataSourceId);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Dataset) {
			log.debug("dataset received");
				
			Patterns.pipe(
				Patterns.ask(database, new RegisterSourceDataset(dataSourceId, (Dataset)msg), 15000),
				getContext().dispatcher()).pipeTo(getSender(), getSelf());
		} else if(msg instanceof Finished) {
			log.debug("harvesting finished");
			
			final ActorRef self = getSelf();
			HarvestLogLine logLine = new HarvestLogLine(GenericEvent.FINISHED, dataSourceId);
			Patterns.ask(database, new StoreLog(logLine), 150000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						log.debug("harvesting of dataSource finished: " + dataSourceId);
						getContext().stop(self);
					}
				}, getContext().dispatcher());			
		} else {
			unhandled(msg);
		}
	}
}
