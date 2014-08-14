package nl.idgis.publisher.service.init;

import java.util.List;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.HarvestJob;
import nl.idgis.publisher.database.messages.ImportJob;
import nl.idgis.publisher.service.init.messages.Initiate;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Initiator extends UntypedActor {
	
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
	public void preStart() throws Exception {
		FiniteDuration interval = Duration.create(10, TimeUnit.SECONDS);
		getContext().system().scheduler().schedule(Duration.Zero(), interval, getSelf(), new Initiate(), getContext().dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Initiate) {
			log.debug("initiating actions");
			
			Patterns.ask(database, new GetHarvestJobs(), 15000)
				.onSuccess(new OnSuccess<Object>() {
					
					@Override
					@SuppressWarnings("unchecked")
					public void onSuccess(Object msg) throws Throwable {
						List<HarvestJob> harvestJobs = (List<HarvestJob>) msg;
						
						for(HarvestJob harvestJob : harvestJobs) {						
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
						List<ImportJob> importJobs = (List<ImportJob>) msg;
						
						for(ImportJob importJob : importJobs) {
							loader.tell(importJob, getSelf());
						}
					}
				}, getContext().dispatcher());			
		} else {
			unhandled(msg);
		}
	}
}
