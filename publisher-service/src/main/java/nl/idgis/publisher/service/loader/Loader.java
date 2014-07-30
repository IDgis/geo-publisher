package nl.idgis.publisher.service.loader;

import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.ImportLogLine;
import nl.idgis.publisher.harvester.messages.RequestDataset;
import nl.idgis.publisher.service.loader.messages.ImportDataset;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Loader extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef database, harvester;

	public Loader(ActorRef database, ActorRef harvester) {
		this.database = database;
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef database, ActorRef harvester) {
		return Props.create(Loader.class, database, harvester);
	}
	
	@Override
	public void preStart() throws Exception {
		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ImportDataset) {
			log.debug("data import requested: " + msg);
			
			final ImportDataset importDataset = (ImportDataset)msg;
			
			ImportLogLine logLine = new ImportLogLine(GenericEvent.STARTED, ((ImportDataset) msg).getDatasetId());
			Patterns.ask(database, new StoreLog(logLine), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						ActorRef session = getContext().actorOf(LoaderSession.props(database));
						harvester.tell(new RequestDataset(importDataset.getDataSourceId(), importDataset.getSourceDatasetId()), session);
					}
				}, getContext().dispatcher());
		} else {
			unhandled(msg);
		}
	}	
}
