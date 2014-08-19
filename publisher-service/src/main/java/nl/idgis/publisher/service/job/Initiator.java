package nl.idgis.publisher.service.job;

import java.util.List;

import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.HarvestJobInfo;
import nl.idgis.publisher.database.messages.ImportJobInfo;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Initiator extends Scheduled {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader;
	
	public Initiator(ActorRef database, ActorRef harvester, ActorRef loader) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader) {
		return Props.create(Initiator.class, database, harvester, loader);
	}

	@Override
	protected void doInitiate() {
		log.debug("initiating jobs");
		
		Patterns.ask(database, new GetHarvestJobs(), 15000)
			.onSuccess(new OnSuccess<Object>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public void onSuccess(Object msg) throws Throwable {
					List<HarvestJobInfo> harvestJobs = (List<HarvestJobInfo>) msg;
					
					for(HarvestJobInfo harvestJob : harvestJobs) {
						log.debug("harvest job received");
						
						harvester.tell(harvestJob, getSelf());
					}
				}
			}, getContext().dispatcher());
		
		Patterns.ask(database, new GetImportJobs(), 15000)
			.onSuccess(new OnSuccess<Object>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public void onSuccess(Object msg) throws Throwable {
					List<ImportJobInfo> importJobs = (List<ImportJobInfo>) msg;
					
					for(ImportJobInfo importJob : importJobs) {
						loader.tell(importJob, getSelf());
					}
				}
			}, getContext().dispatcher());		
	}
}
