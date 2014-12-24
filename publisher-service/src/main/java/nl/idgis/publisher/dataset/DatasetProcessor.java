package nl.idgis.publisher.dataset;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.domain.service.Dataset;

public class DatasetProcessor extends UntypedActor {
	
	private final ActorRef sender, database;
	
	private final String dataSourceId;
	
	public DatasetProcessor(ActorRef sender, ActorRef database, String dataSourceId) {
		this.sender = sender;
		this.database = database;
		this.dataSourceId = dataSourceId;
	}
	
	public static Props props(ActorRef sender, ActorRef database, String dataSourceId) {
		return Props.create(DatasetProcessor.class, sender, database, dataSourceId);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Dataset) {
			handleDataset((Dataset)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleDataset(Dataset msg) {
		
	}
}
