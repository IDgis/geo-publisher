package nl.idgis.publisher.service.loader;

import nl.idgis.publisher.harvester.messages.RequestDataset;
import nl.idgis.publisher.service.loader.messages.ImportDataset;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

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
			
			
			ImportDataset importDataset = (ImportDataset)msg;
			String datasetId = importDataset.getDatasetId();			
			
			harvester.tell(new RequestDataset(
					datasetId, 
					importDataset.getDataSourceId(), 
					importDataset.getSourceDatasetId(),
					LoaderSession.props(datasetId, database)), getSelf());
		} else {
			unhandled(msg);
		}
	}	
}
